package edu.singaporetech.inf2007quiz01

/**
 * JNI Bridge to the Vulkan GPU Compute Engine.
 *
 * Routes addition through a Vulkan compute shader because the CPU
 * was insufficiently dramatic for adding two numbers together.
 */
object GpuBridge {
    init {
        System.loadLibrary("gpu_compute")
    }

    /** Returns true if a Vulkan compute device is available. */
    external fun isVulkanAvailable(): Boolean

    /** Computes a + b on the GPU via Vulkan compute shader. Returns JSON. */
    external fun gpuAdd(a: Int, b: Int): String
}
