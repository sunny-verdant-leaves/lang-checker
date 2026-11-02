#!/bin/bash

DEST_DIR=$1

if [ -z "$DEST_DIR" ]; then
    echo "用法: $0 <目标目录>"
    exit 1
fi

mkdir -p "$DEST_DIR"

find . -path "*/build/libs/*.jar" -o -path "*/build/lib/*.jar" | while read -r jar; do
    # 排除 common 模块 (路径中包含 /common/)
    if [[ "$jar" == *"/common/"* ]]; then
        continue
    fi

    filename=$(basename "$jar")
    
    # 排除特定后缀
    if [[ "$filename" == *"-dev-shadow"* ]] || [[ "$filename" == *"-sources"* ]] || [[ "$filename" == *"-javadoc"* ]]; then
        continue
    fi
    
    mv "$jar" "$DEST_DIR/"
    echo "  [移动] $filename"
done

echo "完成！目标目录：$DEST_DIR"