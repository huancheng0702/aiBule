# -*- coding: utf-8 -*-
"""
修复 AAR/JAR 中非法的 zip 条目。

某些打包工具（如 JDK 的 jar 工具在特定环境下）会给 STORED（未压缩）条目
写入数据描述符（general purpose flag bit 3），这是 ZIP 规范不允许的组合。
Android 构建时的 Jetifier/AAPT2 解析这种 AAR 会直接失败：

    java.util.zip.ZipException: only DEFLATED entries can have EXT descriptor

本脚本将 zip 内的所有条目重新写入：
  - 文件条目统一使用 DEFLATE 压缩，且不写数据描述符
  - 目录条目使用 STORED 但尺寸已知（合法的空条目）
并递归修复嵌套的 .jar / .aar / .zip。

用法:
    python tools/fix-aar-zip.py nativeplugins/CoolWear-BLE/android/CoolWear-BLE.aar
"""
import io
import os
import shutil
import struct
import sys
import tempfile
import zipfile

NESTED_EXTENSIONS = (".jar", ".aar", ".zip")


def count_bad_entries(raw: bytes) -> int:
    bad = 0
    i = raw.find(b"PK\x03\x04")
    while i != -1 and i + 30 <= len(raw):
        (ver, flags, method, mtime, mdate, crc, csize, usize, nlen, elen) = struct.unpack(
            "<HHHHHIIIHH", raw[i + 4 : i + 30]
        )
        if flags & 0x8 and method == 0:
            bad += 1
        i = raw.find(b"PK\x03\x04", i + 4)
    return bad


def is_zip(raw: bytes) -> bool:
    return raw[:4] in (b"PK\x03\x04", b"PK\x05\x06")


def rebuild_zip(raw: bytes) -> bytes:
    zin = zipfile.ZipFile(io.BytesIO(raw))
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if (
                info.filename.lower().endswith(NESTED_EXTENSIONS)
                and is_zip(data)
                and count_bad_entries(data)
            ):
                data = rebuild_zip(data)
            zi = zipfile.ZipInfo(info.filename, info.date_time)
            zi.compress_type = (
                zipfile.ZIP_STORED if info.filename.endswith("/") else zipfile.ZIP_DEFLATED
            )
            zi.external_attr = info.external_attr
            zi.create_system = info.create_system
            zout.writestr(zi, data)
    return out.getvalue()


def fix_file(path: str) -> None:
    with open(path, "rb") as f:
        raw = f.read()

    bad = count_bad_entries(raw)
    print(f"[check] {path}: {bad} 个非法条目 (STORED + data descriptor)")
    if bad == 0:
        print("[skip] 无需修复")
        return

    backup = os.path.join(tempfile.gettempdir(), os.path.basename(path) + ".bak")
    shutil.copy2(path, backup)

    fixed = rebuild_zip(raw)
    with open(path, "wb") as f:
        f.write(fixed)

    with open(path, "rb") as f:
        remaining = count_bad_entries(f.read())
    print(f"[fixed] {path}: 剩余非法条目 {remaining}，原文件备份在 {backup}")
    if remaining:
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python tools/fix-aar-zip.py <a.aar|b.jar> [...]")
        sys.exit(1)
    for arg in sys.argv[1:]:
        fix_file(arg)
