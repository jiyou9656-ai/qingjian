local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = "seckill:stock:" .. voucherId
local orderKey = "seckill:order:" .. voucherId

-- 判断库存是否充足（不存在或不足，返回 1）
local stock = tonumber(redis.call('GET', stockKey))
if (stock == nil or stock <= 0) then
return 1;
end;

-- 判断用户是否下单（重复下单，返回 2）
if (redis.call('SISMEMBER', orderKey, userId) == 1) then
return 2;
end;

-- 下单成功：扣减库存、保存用户。
redis.call('INCRBY', stockKey, -1);
redis.call('SADD', orderKey, userId);
return 0;