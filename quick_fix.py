import os
import shutil
import sqlite3
import mysql.connector

print("快速修复商家图片问题...")

base_dir = r"D:\JavaProject\redis-hmdp\hmdp-front\imgs"
shops_dir = os.path.join(base_dir, "shops")
os.makedirs(shops_dir, exist_ok=True)

# 商家类型配置 (不包含美食和KTV)
shop_types = [
    {"name": "hair", "type_id": 3},
    {"name": "nail", "type_id": 4},
    {"name": "massage", "type_id": 5},
    {"name": "spa", "type_id": 6},
    {"name": "kids", "type_id": 7},
    {"name": "bar", "type_id": 8},
    {"name": "party", "type_id": 9},
    {"name": "gym", "type_id": 10},
]

# 收集所有可用的图片
all_images = []

# 从blogs目录找
blogs_dir = os.path.join(base_dir, "blogs")
for root, dirs, files in os.walk(blogs_dir):
    for file in files:
        if file.lower().endswith(('.jpg', '.jpeg', '.png')):
            all_images.append(os.path.join(root, file))

# 从types目录找
types_dir = os.path.join(base_dir, "types")
for file in os.listdir(types_dir):
    if file.lower().endswith(('.jpg', '.jpeg', '.png')):
        all_images.append(os.path.join(types_dir, file))

print(f"找到 {len(all_images)} 张可用图片")

# 如果没有足够的图片，我们用一个简单的SQL更新方案
if len(all_images) < 5:
    print("\n图片不够，使用在线图片URL方案...")
    db_config = {
        "host": "localhost",
        "user": "root",
        "password": "123456",
        "database": "hmdp",
        "charset": "utf8mb4"
    }
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        
        # 更新typeId 3-10的商家图片
        for type_id in range(3, 11):
            cursor.execute("SELECT id FROM tb_shop WHERE type_id = %s ORDER BY id", (type_id,))
            shops = cursor.fetchall()
            
            for i, shop in enumerate(shops):
                shop_id = shop[0]
                # 使用多个不同的图片源作为备选
                image_url = f"https://picsum.photos/seed/shop{type_id}_{i}/400/300"
                cursor.execute("UPDATE tb_shop SET images = %s WHERE id = %s", (image_url, shop_id))
                print(f"更新商家 {shop_id}")
        
        conn.commit()
        cursor.close()
        conn.close()
        print("完成！已更新所有商家图片为在线图片URL")
        print("现在请刷新浏览器页面")
        
    except Exception as e:
        print(f"数据库错误: {e}")
        import traceback
        traceback.print_exc()
    
    exit()

# 有足够的图片，复制到商家目录
print("\n开始复制图片...")

total_copied = 0

for shop_type in shop_types:
    name = shop_type["name"]
    type_dir = os.path.join(shops_dir, name)
    os.makedirs(type_dir, exist_ok=True)
    
    for i in range(15):
        src_img = all_images[i % len(all_images)]
        dst_img = os.path.join(type_dir, f"{name}_{i+1}.jpg")
        shutil.copy2(src_img, dst_img)
        total_copied += 1

print(f"已复制 {total_copied} 张图片")

# 更新数据库
print("\n开始更新数据库...")

db_config = {
    "host": "localhost",
    "user": "root",
    "password": "123456",
    "database": "hmdp",
    "charset": "utf8mb4"
}

try:
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    total_updated = 0
    
    for shop_type in shop_types:
        name = shop_type["name"]
        type_id = shop_type["type_id"]
        
        cursor.execute("SELECT id FROM tb_shop WHERE type_id = %s ORDER BY id", (type_id,))
        shops = cursor.fetchall()
        
        for i, shop in enumerate(shops):
            shop_id = shop[0]
            image_idx = (i % 15) + 1
            image_path = f"/imgs/shops/{name}/{name}_{image_idx}.jpg"
            
            cursor.execute("UPDATE tb_shop SET images = %s WHERE id = %s", (image_path, shop_id))
            total_updated += 1
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"\n完成！已更新 {total_updated} 个商家")
    print("请刷新浏览器页面！")
    
except Exception as e:
    print(f"数据库错误: {e}")
    import traceback
    traceback.print_exc()
