# Rain and snow — feasibility assessment, 2026-09-19

## Decision: defer implementation for this performance pass

Native-looking, lensed precipitation is feasible. Capturing its geometry is relatively small work; correct transparent composition is the larger addition. With the current1440p renderer still well over the16.7ms budget, I would not add an unmeasured transparency pass now. This is a scope/performance decision, not a claim that rain or snow is impossible or has a measured FPS penalty.

## Evidence from the pinned Minecraft1.21.1 client

Inspected the local named client jar's `WorldRenderer.renderWeather` bytecode (`run/weather-bytecode.txt`, ignored). It uses a camera-centred horizontal radius of5, or10 with Fancy graphics: at most121/441 candidate columns. Biome precipitation type and the motion-blocking heightmap select/clamp them. Each contributing column emits a rain or snow quad with native UV animation, colour, lightmap and texture. The theoretical geometry ceiling is242/882 triangles, before rejected columns. Native geometry capture and reusable uploads are therefore plausible.

The method enables blending and depth testing; ordinary Fast/Fancy mode disables depth writes for precipitation. Multiple transparent surfaces can contribute along one view ray. Our current terrain pass handles an opaque nearest hit plus a single nearest cloud layer. The latter matches the native fancy-cloud depth prepass; it does not reproduce a stack of rain/snow layers. Reusing that one-layer shortcut would alter density and occlusion. A straight-camera overlay also fails where lensing and scene occlusion matter.

## A promising bounded design

1. Capture native weather column parameters and textures. Reuse the moving upload buffers; preserve biome, roof, lighting, wind and time behavior.
2. Keep weather in a small camera-local grid, rather than mixing hundreds of long transparent quads into the large opaque terrain tree. Test only chord portions overlapping that volume. Returning rays must be allowed to re-enter it.
3. Clip weather processing to the nearest opaque hit on each chord, then compose visible transparent intersections in ray order. Compare depth and density against native rendering with lensing disabled before testing bent views.
4. A possible cheaper approximation is order-independent transmittance. For identical layer colour C, composition is exactly `C*(1-product(1-alpha)) + background*product(1-alpha)`. Native colour/light/fog vary, so a weighted-colour version would be an approximation requiring measured image acceptance, especially at mixed rain/snow boundaries. It is not an accepted implementation.
5. Keep the dry-weather path free of the new traversal work, and measure wet-weather cost separately. Test open sky, roofs, terrain foregrounds, mixed biomes and a strongly bent view. No teleport or physical weather-history feature is required beyond the current live snapshot model.

The unresolved work is the transparent compositor, its interaction with clouds/opaque geometry, and wet-weather GPU cost. No rain/snow FPS figure is available because no prototype was benchmarked. Defer rather than ship incorrect layer ordering or assume the small triangle count guarantees a cheap pass.
