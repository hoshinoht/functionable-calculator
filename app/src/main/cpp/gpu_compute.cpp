/**
 * gpu_compute.cpp -- Vulkan Compute Shader Addition Engine
 *
 * Because when your calculator app needs to add 2 + 3, the obvious
 * architectural choice is to spin up an entire GPU compute pipeline,
 * allocate device memory, dispatch a workgroup, and synchronize via
 * a fence. You know, like a professional.
 *
 * This module creates a Vulkan instance, finds a compute-capable GPU,
 * builds a full compute pipeline with an embedded SPIR-V shader, and
 * dispatches exactly ONE workgroup to perform a single integer addition.
 *
 * Peak engineering. No notes.
 */

#include <vulkan/vulkan.h>
#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <android/log.h>
#include <chrono>

#define LOG_TAG "VulkanAdditionEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// SPIR-V Compute Shader -- Hand-Assembled Binary
//
// This is the compiled SPIR-V for:
//
//   #version 450
//   layout(local_size_x = 1) in;
//   layout(binding = 0) buffer Buf { int data[]; };
//   void main() { data[2] = data[0] + data[1]; }
//
// Yes, we hand-encoded a GPU shader binary to add two integers.
// This is what happens when you let architects near a calculator.
// ============================================================================
// clang-format off
static const uint32_t k_add_shader_spirv[] = {
    // ---- Header ----
    0x07230203, // Magic number
    0x00010000, // SPIR-V version 1.0
    0x00000000, // Generator (0 = hand-coded, obviously)
    0x00000014, // Bound = 20 (IDs 0..19)
    0x00000000, // Schema

    // ---- OpCapability Shader ----
    // Word count=2, Opcode=17
    0x00020011, 0x00000001,

    // ---- OpMemoryModel Logical GLSL450 ----
    // Word count=3, Opcode=14
    0x0003000E, 0x00000000, 0x00000001,

    // ---- OpEntryPoint GLCompute %3 "main" ----
    // Word count=5, Opcode=15
    // ExecutionModel=5 (GLCompute), %3=main, "main" = 4 chars + null = 2 words
    0x0005000F, 0x00000005, 0x00000003, 0x6E69616D, 0x00000000,

    // ---- OpExecutionMode %3 LocalSize 1 1 1 ----
    // Word count=6, Opcode=16
    0x00060010, 0x00000003, 0x00000011, 0x00000001, 0x00000001, 0x00000001,

    // ---- Decorations ----

    // OpDecorate %5 (runtime array) ArrayStride 4
    // Word count=4, Opcode=71
    0x00040047, 0x00000005, 0x00000006, 0x00000004,

    // OpMemberDecorate %6 0 Offset 0
    // Word count=5, Opcode=72
    0x00050048, 0x00000006, 0x00000000, 0x00000023, 0x00000000,

    // OpDecorate %6 BufferBlock
    // Word count=3, Opcode=71
    0x00030047, 0x00000006, 0x00000003,

    // OpDecorate %8 DescriptorSet 0
    // Word count=4, Opcode=71
    0x00040047, 0x00000008, 0x00000022, 0x00000000,

    // OpDecorate %8 Binding 0
    // Word count=4, Opcode=71
    0x00040047, 0x00000008, 0x00000021, 0x00000000,

    // ---- Type declarations ----

    // OpTypeVoid %1
    // Word count=2, Opcode=19
    0x00020013, 0x00000001,

    // OpTypeFunction %2 %1   (void function type)
    // Word count=3, Opcode=33
    0x00030021, 0x00000002, 0x00000001,

    // OpTypeInt %4 32 1   (32-bit signed int)
    // Word count=4, Opcode=21
    0x00040015, 0x00000004, 0x00000020, 0x00000001,

    // OpTypeRuntimeArray %5 %4   (runtime array of int)
    // Word count=3, Opcode=29
    0x0003001D, 0x00000005, 0x00000004,

    // OpTypeStruct %6 %5   (struct containing runtime array)
    // Word count=3, Opcode=30
    0x0003001E, 0x00000006, 0x00000005,

    // OpTypePointer %7 Uniform %6   (pointer to struct, Uniform storage class=2)
    // Word count=4, Opcode=32
    0x00040020, 0x00000007, 0x00000002, 0x00000006,

    // OpTypePointer %12 Uniform %4   (pointer to int, Uniform storage class=2)
    // Word count=4, Opcode=32
    0x00040020, 0x0000000C, 0x00000002, 0x00000004,

    // ---- Constants ----

    // OpConstant %4 %9 0
    // Word count=4, Opcode=43
    0x0004002B, 0x00000004, 0x00000009, 0x00000000,

    // OpConstant %4 %10 1
    // Word count=4, Opcode=43
    0x0004002B, 0x00000004, 0x0000000A, 0x00000001,

    // OpConstant %4 %11 2
    // Word count=4, Opcode=43
    0x0004002B, 0x00000004, 0x0000000B, 0x00000002,

    // ---- Variable ----

    // OpVariable %7 %8 Uniform   (the buffer variable)
    // Word count=4, Opcode=59
    0x0004003B, 0x00000007, 0x00000008, 0x00000002,

    // ---- Function ----

    // OpFunction %1 %3 None %2
    // Word count=5, Opcode=54
    0x00050036, 0x00000001, 0x00000003, 0x00000000, 0x00000002,

    // OpLabel %13
    // Word count=2, Opcode=248
    0x000200F8, 0x0000000D,

    // OpAccessChain %12 %14 %8 %9 %9   → &data[0]
    // Word count=6, Opcode=65
    0x00060041, 0x0000000C, 0x0000000E, 0x00000008, 0x00000009, 0x00000009,

    // OpLoad %4 %15 %14              → load data[0]
    // Word count=4, Opcode=61
    0x0004003D, 0x00000004, 0x0000000F, 0x0000000E,

    // OpAccessChain %12 %16 %8 %9 %10  → &data[1]
    // Word count=6, Opcode=65
    0x00060041, 0x0000000C, 0x00000010, 0x00000008, 0x00000009, 0x0000000A,

    // OpLoad %4 %17 %16              → load data[1]
    // Word count=4, Opcode=61
    0x0004003D, 0x00000004, 0x00000011, 0x00000010,

    // OpIAdd %4 %18 %15 %17          → data[0] + data[1]
    // Word count=5, Opcode=128
    0x00050080, 0x00000004, 0x00000012, 0x0000000F, 0x00000011,

    // OpAccessChain %12 %19 %8 %9 %11  → &data[2]
    // Word count=6, Opcode=65
    0x00060041, 0x0000000C, 0x00000013, 0x00000008, 0x00000009, 0x0000000B,

    // OpStore %19 %18                → data[2] = sum
    // Word count=3, Opcode=62
    0x0003003E, 0x00000013, 0x00000012,

    // OpReturn
    // Word count=1, Opcode=253
    0x000100FD,

    // OpFunctionEnd
    // Word count=1, Opcode=56
    0x00010038,
};
// clang-format on

// ============================================================================
// Vulkan helper: find a queue family with VK_QUEUE_COMPUTE_BIT
// ============================================================================
static int32_t findComputeQueueFamily(VkPhysicalDevice physDev) {
    uint32_t count = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physDev, &count, nullptr);
    if (count == 0) return -1;

    std::vector<VkQueueFamilyProperties> props(count);
    vkGetPhysicalDeviceQueueFamilyProperties(physDev, &count, props.data());

    for (uint32_t i = 0; i < count; ++i) {
        if (props[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
            return static_cast<int32_t>(i);
        }
    }
    return -1;
}

// ============================================================================
// JNI: isVulkanAvailable
//
// Attempts to create a VkInstance, enumerate physical devices, and find at
// least one with a compute queue. Because we need to validate the GPU's
// willingness to participate in basic arithmetic.
// ============================================================================
extern "C" JNIEXPORT jboolean JNICALL
Java_edu_singaporetech_inf2007quiz01_GpuBridge_isVulkanAvailable(
        JNIEnv* /*env*/, jobject /*thiz*/) {

    LOGI("Probing for Vulkan compute capability... for addition.");

    VkApplicationInfo appInfo{};
    appInfo.sType              = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName   = "CalcuLux Vulkan Probe";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName        = "AdditionEngine";
    appInfo.engineVersion      = VK_MAKE_VERSION(1, 0, 0);
    appInfo.apiVersion         = VK_API_VERSION_1_0;

    VkInstanceCreateInfo createInfo{};
    createInfo.sType            = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    if (vkCreateInstance(&createInfo, nullptr, &instance) != VK_SUCCESS) {
        LOGW("Vulkan instance creation failed. The GPU refuses to do math.");
        return JNI_FALSE;
    }

    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        LOGW("No physical devices found. The silicon has abandoned us.");
        vkDestroyInstance(instance, nullptr);
        return JNI_FALSE;
    }

    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());

    bool found = false;
    for (auto& dev : devices) {
        if (findComputeQueueFamily(dev) >= 0) {
            found = true;
            break;
        }
    }

    // Clean up our probe instance -- we're responsible citizens of the GPU
    vkDestroyInstance(instance, nullptr);

    if (found) {
        LOGI("Vulkan compute device located. Addition can proceed with dignity.");
    } else {
        LOGW("No compute queue found. The GPU exists but declines arithmetic.");
    }

    return found ? JNI_TRUE : JNI_FALSE;
}

// ============================================================================
// JNI: gpuAdd
//
// The main event. Creates an entire Vulkan compute pipeline to add two
// integers together. This is roughly 400 lines of code to replace the
// single CPU instruction: ADD r0, r1, r2.
//
// But hey, at least it's parallel. For one workgroup. Of one invocation.
// ============================================================================
extern "C" JNIEXPORT jstring JNICALL
Java_edu_singaporetech_inf2007quiz01_GpuBridge_gpuAdd(
        JNIEnv* env, jobject /*thiz*/, jint a, jint b) {

    LOGI("gpuAdd(%d, %d) -- Initiating Vulkan pipeline for integer addition.", a, b);

    // -- Convenience lambda for returning error JSON --
    auto errorJson = [&](const char* msg) -> jstring {
        std::string json = "{\"result\":0,\"device\":\"none\","
                           "\"dispatch_us\":0,\"verified\":false,"
                           "\"error\":\"";
        json += msg;
        json += "\"}";
        LOGE("gpuAdd failed: %s", msg);
        return env->NewStringUTF(json.c_str());
    };

    // ================================================================
    // Step 1: Create a Vulkan Instance
    // Every great addition starts with bureaucracy.
    // ================================================================
    VkApplicationInfo appInfo{};
    appInfo.sType              = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName   = "CalcuLux GPU Addition";
    appInfo.applicationVersion = VK_MAKE_VERSION(1, 0, 0);
    appInfo.pEngineName        = "ArithmeticShaderEngine";
    appInfo.engineVersion      = VK_MAKE_VERSION(42, 0, 0); // Semantic versioning is a suggestion
    appInfo.apiVersion         = VK_API_VERSION_1_0;

    VkInstanceCreateInfo instInfo{};
    instInfo.sType            = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    instInfo.pApplicationInfo = &appInfo;

    VkInstance instance = VK_NULL_HANDLE;
    if (vkCreateInstance(&instInfo, nullptr, &instance) != VK_SUCCESS) {
        return errorJson("Vulkan unavailable");
    }

    // ================================================================
    // Step 2: Find a physical device with compute capability
    // We need the full might of the GPU for this addition.
    // ================================================================
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    if (deviceCount == 0) {
        vkDestroyInstance(instance, nullptr);
        return errorJson("No physical devices");
    }

    std::vector<VkPhysicalDevice> physicalDevices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, physicalDevices.data());

    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    int32_t computeQueueFamily = -1;

    for (auto& dev : physicalDevices) {
        int32_t qf = findComputeQueueFamily(dev);
        if (qf >= 0) {
            physicalDevice = dev;
            computeQueueFamily = qf;
            break; // First compute-capable device wins the honour of adding two numbers
        }
    }

    if (physicalDevice == VK_NULL_HANDLE) {
        vkDestroyInstance(instance, nullptr);
        return errorJson("No compute-capable device");
    }

    // Get the device name -- the GPU deserves credit for this heroic arithmetic
    VkPhysicalDeviceProperties deviceProps;
    vkGetPhysicalDeviceProperties(physicalDevice, &deviceProps);
    std::string deviceName(deviceProps.deviceName);
    LOGI("Selected GPU: %s -- it has no idea what's about to happen.", deviceName.c_str());

    // ================================================================
    // Step 3: Create a logical device with a compute queue
    // Because raw hardware access is for barbarians.
    // ================================================================
    float queuePriority = 1.0f; // Maximum priority for our critical addition operation

    VkDeviceQueueCreateInfo queueInfo{};
    queueInfo.sType            = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueInfo.queueFamilyIndex = static_cast<uint32_t>(computeQueueFamily);
    queueInfo.queueCount       = 1;
    queueInfo.pQueuePriorities = &queuePriority;

    VkDeviceCreateInfo deviceCreateInfo{};
    deviceCreateInfo.sType                = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    deviceCreateInfo.queueCreateInfoCount = 1;
    deviceCreateInfo.pQueueCreateInfos    = &queueInfo;

    VkDevice device = VK_NULL_HANDLE;
    if (vkCreateDevice(physicalDevice, &deviceCreateInfo, nullptr, &device) != VK_SUCCESS) {
        vkDestroyInstance(instance, nullptr);
        return errorJson("Failed to create logical device");
    }

    VkQueue computeQueue = VK_NULL_HANDLE;
    vkGetDeviceQueue(device, static_cast<uint32_t>(computeQueueFamily), 0, &computeQueue);

    // ================================================================
    // Step 4: Allocate a buffer for 3 ints: [a, b, result]
    // Allocating GPU memory for what is fundamentally addition.
    // ================================================================
    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType       = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size        = sizeof(int32_t) * 3; // 12 bytes. Worth it.
    bufferInfo.usage       = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

    VkBuffer buffer = VK_NULL_HANDLE;
    if (vkCreateBuffer(device, &bufferInfo, nullptr, &buffer) != VK_SUCCESS) {
        vkDestroyDevice(device, nullptr);
        vkDestroyInstance(instance, nullptr);
        return errorJson("Failed to create buffer for 12 bytes of critical data");
    }

    // Figure out what memory type the GPU wants for our 12 bytes
    VkMemoryRequirements memReqs;
    vkGetBufferMemoryRequirements(device, buffer, &memReqs);

    VkPhysicalDeviceMemoryProperties memProps;
    vkGetPhysicalDeviceMemoryProperties(physicalDevice, &memProps);

    uint32_t memTypeIndex = UINT32_MAX;
    VkMemoryPropertyFlags desiredFlags =
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;

    for (uint32_t i = 0; i < memProps.memoryTypeCount; ++i) {
        if ((memReqs.memoryTypeBits & (1u << i)) &&
            (memProps.memoryTypes[i].propertyFlags & desiredFlags) == desiredFlags) {
            memTypeIndex = i;
            break;
        }
    }

    if (memTypeIndex == UINT32_MAX) {
        // The GPU has memory but refuses to share it. Classic.
        vkDestroyBuffer(device, buffer, nullptr);
        vkDestroyDevice(device, nullptr);
        vkDestroyInstance(instance, nullptr);
        return errorJson("No suitable memory type for host-visible buffer");
    }

    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType           = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize  = memReqs.size;
    allocInfo.memoryTypeIndex = memTypeIndex;

    VkDeviceMemory bufferMemory = VK_NULL_HANDLE;
    if (vkAllocateMemory(device, &allocInfo, nullptr, &bufferMemory) != VK_SUCCESS) {
        vkDestroyBuffer(device, buffer, nullptr);
        vkDestroyDevice(device, nullptr);
        vkDestroyInstance(instance, nullptr);
        return errorJson("Failed to allocate device memory");
    }

    vkBindBufferMemory(device, buffer, bufferMemory, 0);

    // Write our operands into GPU memory -- the most over-engineered memcpy
    {
        void* mapped = nullptr;
        vkMapMemory(device, bufferMemory, 0, sizeof(int32_t) * 3, 0, &mapped);
        int32_t* data = static_cast<int32_t*>(mapped);
        data[0] = static_cast<int32_t>(a);
        data[1] = static_cast<int32_t>(b);
        data[2] = 0; // Result placeholder -- the GPU will grace us with the answer
        vkUnmapMemory(device, bufferMemory);
    }

    // ================================================================
    // Step 5: Create the compute shader module from embedded SPIR-V
    // Loading a hand-assembled GPU binary to add two numbers.
    // ================================================================
    VkShaderModuleCreateInfo shaderInfo{};
    shaderInfo.sType    = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    shaderInfo.codeSize = sizeof(k_add_shader_spirv);
    shaderInfo.pCode    = k_add_shader_spirv;

    VkShaderModule shaderModule = VK_NULL_HANDLE;
    if (vkCreateShaderModule(device, &shaderInfo, nullptr, &shaderModule) != VK_SUCCESS) {
        vkFreeMemory(device, bufferMemory, nullptr);
        vkDestroyBuffer(device, buffer, nullptr);
        vkDestroyDevice(device, nullptr);
        vkDestroyInstance(instance, nullptr);
        return errorJson("Failed to create shader module -- the SPIR-V displeases the driver");
    }

    // ================================================================
    // Step 6: Create descriptor set layout, pipeline layout, and pipeline
    // This is the part where enterprise architecture really shines.
    // ================================================================

    // Descriptor set layout: one storage buffer at binding 0
    VkDescriptorSetLayoutBinding layoutBinding{};
    layoutBinding.binding         = 0;
    layoutBinding.descriptorType  = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    layoutBinding.descriptorCount = 1;
    layoutBinding.stageFlags      = VK_SHADER_STAGE_COMPUTE_BIT;

    VkDescriptorSetLayoutCreateInfo descLayoutInfo{};
    descLayoutInfo.sType        = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    descLayoutInfo.bindingCount = 1;
    descLayoutInfo.pBindings    = &layoutBinding;

    VkDescriptorSetLayout descriptorSetLayout = VK_NULL_HANDLE;
    vkCreateDescriptorSetLayout(device, &descLayoutInfo, nullptr, &descriptorSetLayout);

    // Pipeline layout -- the bureaucratic wrapper around the bureaucratic wrapper
    VkPipelineLayoutCreateInfo pipelineLayoutInfo{};
    pipelineLayoutInfo.sType          = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pipelineLayoutInfo.setLayoutCount = 1;
    pipelineLayoutInfo.pSetLayouts    = &descriptorSetLayout;

    VkPipelineLayout pipelineLayout = VK_NULL_HANDLE;
    vkCreatePipelineLayout(device, &pipelineLayoutInfo, nullptr, &pipelineLayout);

    // Compute pipeline -- the crowning achievement of this arithmetic endeavour
    VkComputePipelineCreateInfo pipelineInfo{};
    pipelineInfo.sType              = VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO;
    pipelineInfo.stage.sType        = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    pipelineInfo.stage.stage        = VK_SHADER_STAGE_COMPUTE_BIT;
    pipelineInfo.stage.module       = shaderModule;
    pipelineInfo.stage.pName        = "main";
    pipelineInfo.layout             = pipelineLayout;

    VkPipeline pipeline = VK_NULL_HANDLE;
    if (vkCreateComputePipelines(device, VK_NULL_HANDLE, 1, &pipelineInfo,
                                  nullptr, &pipeline) != VK_SUCCESS) {
        vkDestroyShaderModule(device, shaderModule, nullptr);
        vkDestroyPipelineLayout(device, pipelineLayout, nullptr);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, nullptr);
        vkFreeMemory(device, bufferMemory, nullptr);
        vkDestroyBuffer(device, buffer, nullptr);
        vkDestroyDevice(device, nullptr);
        vkDestroyInstance(instance, nullptr);
        return errorJson("Failed to create compute pipeline");
    }

    // ================================================================
    // Step 6b: Allocate and update a descriptor set
    // Telling the GPU where our 12 bytes of critical data live.
    // ================================================================
    VkDescriptorPoolSize poolSize{};
    poolSize.type            = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    poolSize.descriptorCount = 1;

    VkDescriptorPoolCreateInfo poolInfo{};
    poolInfo.sType         = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    poolInfo.maxSets       = 1;
    poolInfo.poolSizeCount = 1;
    poolInfo.pPoolSizes    = &poolSize;

    VkDescriptorPool descriptorPool = VK_NULL_HANDLE;
    vkCreateDescriptorPool(device, &poolInfo, nullptr, &descriptorPool);

    VkDescriptorSetAllocateInfo descAllocInfo{};
    descAllocInfo.sType              = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    descAllocInfo.descriptorPool     = descriptorPool;
    descAllocInfo.descriptorSetCount = 1;
    descAllocInfo.pSetLayouts        = &descriptorSetLayout;

    VkDescriptorSet descriptorSet = VK_NULL_HANDLE;
    vkAllocateDescriptorSets(device, &descAllocInfo, &descriptorSet);

    VkDescriptorBufferInfo descBufferInfo{};
    descBufferInfo.buffer = buffer;
    descBufferInfo.offset = 0;
    descBufferInfo.range  = sizeof(int32_t) * 3;

    VkWriteDescriptorSet writeDesc{};
    writeDesc.sType           = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    writeDesc.dstSet          = descriptorSet;
    writeDesc.dstBinding      = 0;
    writeDesc.descriptorCount = 1;
    writeDesc.descriptorType  = VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
    writeDesc.pBufferInfo     = &descBufferInfo;

    vkUpdateDescriptorSets(device, 1, &writeDesc, 0, nullptr);

    // ================================================================
    // Step 7: Record a command buffer
    // A single dispatch command, wrapped in the full ceremony of
    // command pool creation, command buffer allocation, and recording.
    // ================================================================
    VkCommandPoolCreateInfo cmdPoolInfo{};
    cmdPoolInfo.sType            = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    cmdPoolInfo.queueFamilyIndex = static_cast<uint32_t>(computeQueueFamily);

    VkCommandPool commandPool = VK_NULL_HANDLE;
    vkCreateCommandPool(device, &cmdPoolInfo, nullptr, &commandPool);

    VkCommandBufferAllocateInfo cmdBufInfo{};
    cmdBufInfo.sType              = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    cmdBufInfo.commandPool        = commandPool;
    cmdBufInfo.level              = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    cmdBufInfo.commandBufferCount = 1;

    VkCommandBuffer commandBuffer = VK_NULL_HANDLE;
    vkAllocateCommandBuffers(device, &cmdBufInfo, &commandBuffer);

    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;

    vkBeginCommandBuffer(commandBuffer, &beginInfo);
    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline);
    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE,
                            pipelineLayout, 0, 1, &descriptorSet, 0, nullptr);

    // The moment we've all been waiting for -- dispatching ONE workgroup
    // to perform ONE addition. Peak GPU utilization.
    vkCmdDispatch(commandBuffer, 1, 1, 1);
    vkEndCommandBuffer(commandBuffer);

    // ================================================================
    // Step 8: Submit and wait on a fence
    // We synchronize the CPU and GPU for one integer addition.
    // ================================================================
    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;

    VkFence fence = VK_NULL_HANDLE;
    vkCreateFence(device, &fenceInfo, nullptr, &fence);

    VkSubmitInfo submitInfo{};
    submitInfo.sType              = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers    = &commandBuffer;

    // Start the high-precision timer -- because we need microsecond
    // resolution on how long it takes the GPU to add two numbers
    auto t0 = std::chrono::high_resolution_clock::now();

    vkQueueSubmit(computeQueue, 1, &submitInfo, fence);
    vkWaitForFences(device, 1, &fence, VK_TRUE, UINT64_MAX);

    auto t1 = std::chrono::high_resolution_clock::now();
    auto dispatchUs = std::chrono::duration_cast<std::chrono::microseconds>(t1 - t0).count();

    LOGI("GPU dispatch completed in %lld us. A CPU could have done this in ~1 ns.", (long long)dispatchUs);

    // ================================================================
    // Step 9: Read back the result
    // The GPU has spoken. Let us receive its wisdom.
    // ================================================================
    int32_t result = 0;
    bool verified = false;
    {
        void* mapped = nullptr;
        vkMapMemory(device, bufferMemory, 0, sizeof(int32_t) * 3, 0, &mapped);
        int32_t* data = static_cast<int32_t*>(mapped);
        result = data[2];
        // Verify the GPU didn't hallucinate -- trust but verify
        verified = (result == (static_cast<int32_t>(a) + static_cast<int32_t>(b)));
        if (!verified) {
            LOGE("GPU VERIFICATION FAILED: expected %d + %d = %d, got %d. The silicon lies.",
                 a, b, a + b, result);
        }
        vkUnmapMemory(device, bufferMemory);
    }

    // ================================================================
    // Step 10: Clean up ALL Vulkan resources
    // Being a responsible citizen after our brief, extravagant
    // occupation of the graphics hardware for arithmetic purposes.
    // ================================================================
    vkDestroyFence(device, fence, nullptr);
    vkDestroyCommandPool(device, commandPool, nullptr);
    vkDestroyPipeline(device, pipeline, nullptr);
    vkDestroyDescriptorPool(device, descriptorPool, nullptr);
    vkDestroyPipelineLayout(device, pipelineLayout, nullptr);
    vkDestroyDescriptorSetLayout(device, descriptorSetLayout, nullptr);
    vkDestroyShaderModule(device, shaderModule, nullptr);
    vkFreeMemory(device, bufferMemory, nullptr);
    vkDestroyBuffer(device, buffer, nullptr);
    vkDestroyDevice(device, nullptr);
    vkDestroyInstance(instance, nullptr);

    LOGI("All Vulkan resources released. The GPU is free to render cat videos again.");

    // ================================================================
    // Step 11: Build the JSON response
    // Hand-crafted artisanal JSON, because pulling in a JSON library
    // for 5 fields would be under-engineering.
    // ================================================================

    // Escape any quotes in device name (unlikely but we're thorough engineers)
    std::string escapedName;
    for (char c : deviceName) {
        if (c == '"') escapedName += "\\\"";
        else if (c == '\\') escapedName += "\\\\";
        else escapedName += c;
    }

    std::string json = "{\"result\":" + std::to_string(result) +
                       ",\"device\":\"" + escapedName + "\"" +
                       ",\"dispatch_us\":" + std::to_string(dispatchUs) +
                       ",\"verified\":" + (verified ? "true" : "false") + "}";

    LOGI("Returning: %s", json.c_str());
    return env->NewStringUTF(json.c_str());
}
