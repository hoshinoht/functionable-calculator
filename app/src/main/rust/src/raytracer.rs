/// CalcuLux Raytracer — Software CPU Raytracer
///
/// Because a calculator should obviously render a 3D scene for every computation.
/// 160×120 pixels × 4 channels = 76,800 bytes of pure enterprise value.
/// No GPU, no hardware acceleration, no shame.

use std::ops::{Add, Neg, Sub};

// ── Vector Math ───────────────────────────────────────────────────────────────

#[derive(Clone, Copy)]
struct Vec3 {
    x: f32,
    y: f32,
    z: f32,
}

impl Vec3 {
    fn new(x: f32, y: f32, z: f32) -> Self {
        Vec3 { x, y, z }
    }

    fn dot(self, other: Vec3) -> f32 {
        self.x * other.x + self.y * other.y + self.z * other.z
    }

    fn len(self) -> f32 {
        self.dot(self).sqrt()
    }

    fn normalize(self) -> Vec3 {
        let l = self.len();
        if l < 1e-8 {
            Vec3::new(0.0, 1.0, 0.0)
        } else {
            Vec3::new(self.x / l, self.y / l, self.z / l)
        }
    }

    fn scale(self, t: f32) -> Vec3 {
        Vec3::new(self.x * t, self.y * t, self.z * t)
    }

    fn clamp(self, lo: f32, hi: f32) -> Vec3 {
        Vec3::new(
            self.x.clamp(lo, hi),
            self.y.clamp(lo, hi),
            self.z.clamp(lo, hi),
        )
    }
}

impl Add for Vec3 {
    type Output = Vec3;
    fn add(self, rhs: Vec3) -> Vec3 {
        Vec3::new(self.x + rhs.x, self.y + rhs.y, self.z + rhs.z)
    }
}

impl Sub for Vec3 {
    type Output = Vec3;
    fn sub(self, rhs: Vec3) -> Vec3 {
        Vec3::new(self.x - rhs.x, self.y - rhs.y, self.z - rhs.z)
    }
}

impl Neg for Vec3 {
    type Output = Vec3;
    fn neg(self) -> Vec3 {
        Vec3::new(-self.x, -self.y, -self.z)
    }
}

// ── Ray & Scene primitives ────────────────────────────────────────────────────

struct Ray {
    origin: Vec3,
    dir: Vec3,
}

impl Ray {
    fn at(&self, t: f32) -> Vec3 {
        self.origin + self.dir.scale(t)
    }
}

struct Sphere {
    centre: Vec3,
    radius: f32,
    colour: Vec3,
    shininess: f32,
}

fn hit_sphere(sphere: &Sphere, ray: &Ray, t_min: f32, t_max: f32) -> Option<f32> {
    let oc = ray.origin - sphere.centre;
    let a = ray.dir.dot(ray.dir);
    let half_b = oc.dot(ray.dir);
    let c = oc.dot(oc) - sphere.radius * sphere.radius;
    let disc = half_b * half_b - a * c;
    if disc < 0.0 {
        return None;
    }
    let sq = disc.sqrt();
    let t1 = (-half_b - sq) / a;
    if t1 > t_min && t1 < t_max {
        return Some(t1);
    }
    let t2 = (-half_b + sq) / a;
    if t2 > t_min && t2 < t_max {
        return Some(t2);
    }
    None
}

// ── Shading ───────────────────────────────────────────────────────────────────

fn trace(ray: &Ray, spheres: &[Sphere], light: Vec3) -> Vec3 {
    const AMBIENT: f32 = 0.15;

    // Find closest intersection
    let mut closest_t = f32::MAX;
    let mut hit_idx: Option<usize> = None;
    for (i, sphere) in spheres.iter().enumerate() {
        if let Some(t) = hit_sphere(sphere, ray, 0.001, closest_t) {
            closest_t = t;
            hit_idx = Some(i);
        }
    }

    let Some(idx) = hit_idx else {
        // Sky gradient: white at horizon, light blue at top
        let unit = ray.dir.normalize();
        let t = 0.5 * (unit.y + 1.0);
        return Vec3::new(1.0 - t * 0.3, 1.0 - t * 0.1, 1.0);
    };

    let sphere = &spheres[idx];
    let hit_point = ray.at(closest_t);
    let normal = (hit_point - sphere.centre).normalize();

    // Shadow ray toward the light
    let light_dir = (light - hit_point).normalize();
    let light_dist = (light - hit_point).len();
    let shadow_ray = Ray {
        origin: hit_point + normal.scale(1e-4),
        dir: light_dir,
    };
    let in_shadow = spheres.iter().enumerate().any(|(i, s)| {
        i != idx && hit_sphere(s, &shadow_ray, 0.001, light_dist).is_some()
    });

    let base = sphere.colour;
    let ambience = base.scale(AMBIENT);

    if in_shadow {
        return ambience;
    }

    // Lambertian diffuse
    let diff = normal.dot(light_dir).max(0.0);
    let diffuse = base.scale(diff);

    // Blinn-Phong specular
    let view_dir = (-ray.dir).normalize();
    let half_vec = (light_dir + view_dir).normalize();
    let spec = normal.dot(half_vec).max(0.0).powf(sphere.shininess);
    let specular = Vec3::new(spec, spec, spec);

    (ambience + diffuse + specular).clamp(0.0, 1.0)
}

// ── Public entry point ────────────────────────────────────────────────────────

/// Render a 160×120 scene seeded by the calculator result.
/// Returns 160 × 120 × 4 = 76,800 bytes (R, G, B, A).
pub fn render_scene(seed: f32) -> Vec<u8> {
    const WIDTH: usize = 160;
    const HEIGHT: usize = 120;

    // Seed drives sphere radii and y-positions so every result looks different.
    // Use sin() to keep variations bounded regardless of result magnitude.
    let seed_norm = (seed.abs() * 0.37).sin().abs(); // 0.0..1.0, smooth variation

    let spheres = vec![
        Sphere {
            centre: Vec3::new(-1.2, 0.5 + seed_norm * 0.5, -1.0),
            radius: 0.6 + seed_norm * 0.3,
            colour: Vec3::new(0.85, 0.2, 0.2), // red
            shininess: 32.0,
        },
        Sphere {
            centre: Vec3::new(0.0, 0.2, -2.0),
            radius: 0.8,
            colour: Vec3::new(0.2, 0.75, 0.3), // green
            shininess: 64.0,
        },
        Sphere {
            centre: Vec3::new(1.2, 0.5 - seed_norm * 0.5, -1.0),
            radius: 0.5 + seed_norm * 0.2,
            colour: Vec3::new(0.2, 0.4, 0.9), // blue
            shininess: 128.0,
        },
        Sphere {
            centre: Vec3::new(0.0, -100.5, -1.0),
            radius: 100.0,
            colour: Vec3::new(0.75, 0.75, 0.75), // grey ground
            shininess: 8.0,
        },
    ];

    let light = Vec3::new(2.0, 3.0, -1.0);

    // Pinhole camera at (0,1,−5) looking toward origin
    let cam_origin = Vec3::new(0.0, 1.0, -5.0);
    let focal_length = 3.5_f32;
    let viewport_height = 2.0_f32;
    let viewport_width = viewport_height * (WIDTH as f32 / HEIGHT as f32);

    let horizontal = Vec3::new(viewport_width, 0.0, 0.0);
    let vertical = Vec3::new(0.0, viewport_height, 0.0);
    let lower_left = cam_origin
        - horizontal.scale(0.5)
        - vertical.scale(0.5)
        + Vec3::new(0.0, 0.0, focal_length);

    let mut pixels = vec![0u8; WIDTH * HEIGHT * 4];

    for j in 0..HEIGHT {
        for i in 0..WIDTH {
            let u = i as f32 / (WIDTH - 1) as f32;
            let v = (HEIGHT - 1 - j) as f32 / (HEIGHT - 1) as f32;
            let dir = lower_left + horizontal.scale(u) + vertical.scale(v) - cam_origin;
            let ray = Ray { origin: cam_origin, dir: dir.normalize() };
            let colour = trace(&ray, &spheres, light);

            let base = (j * WIDTH + i) * 4;
            pixels[base]     = (colour.x * 255.0) as u8;
            pixels[base + 1] = (colour.y * 255.0) as u8;
            pixels[base + 2] = (colour.z * 255.0) as u8;
            pixels[base + 3] = 255;
        }
    }

    pixels
}
