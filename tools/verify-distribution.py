"""Inspect a remapped mod jar; no Minecraft installation or third-party Python modules."""
import argparse
import io
import json
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("jar")
parser.add_argument("--rtx", action="store_true")
args = parser.parse_args()
with zipfile.ZipFile(args.jar) as jar:
    names = set(jar.namelist())
    metadata = json.loads(jar.read("fabric.mod.json"))
    assert metadata["id"] == "interstellar"
    assert metadata["depends"]["minecraft"] == "1.21.1"
    bundled = metadata.get("jars", [])
    assert ("interstellar-rtx.marker" in names) == args.rtx
    assert ("VulkanWorldBackend.class" in names) == args.rtx
    if args.rtx:
        assert len(bundled) == 3, bundled
        ids, contents = set(), set()
        for item in bundled:
            with zipfile.ZipFile(io.BytesIO(jar.read(item["file"]))) as library:
                mod = json.loads(library.read("fabric.mod.json"))
                assert mod["id"] not in ids, "Duplicate nested mod ID"
                ids.add(mod["id"])
                assert mod["version"] == "3.3.3"
                contents.update(library.namelist())
        for name in ("org/lwjgl/vulkan/VK.class", "org/lwjgl/util/shaderc/Shaderc.class",
                     "windows/x64/org/lwjgl/shaderc/shaderc.dll"):
            assert name in contents, name
        assert "org/lwjgl/system/MemoryStack.class" not in contents, "Do not duplicate Minecraft's LWJGL core"
        for notice in ("LWJGL.txt", "shaderc.txt", "Khronos.txt"):
            assert len(jar.read("META-INF/licenses/interstellar-rtx/" + notice)) > 100
        assert "RtxShaderCompiler.class" in names
    else:
        assert not bundled, "OpenGL build must have no optional nested libraries"
        assert not any("shaderc" in n.lower() or "vulkan" in n.lower() or n.endswith(".dll") for n in names)
print(f"Distribution verified: {args.jar} ({'RTX + OpenGL' if args.rtx else 'OpenGL only'})")
