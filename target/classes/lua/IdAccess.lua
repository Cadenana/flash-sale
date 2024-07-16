--获取KEY
local key1 = KEYS[1]
-- Lua脚本可以在Redis服务器端一次性执行，Redis会保证脚本内的一系列命令在单个步骤中完成
local val = redis.call('incr', key1)
local ttl = redis.call('ttl', key1)
--获取ARGV内的参数并打印
local expire = tonumber(ARGV[1])
local times = tonumber(ARGV[2])
redis.log(redis.LOG_DEBUG, tostring(times))
redis.log(redis.LOG_DEBUG, tostring(expire))
redis.log(redis.LOG_NOTICE, "incr "..key1.." "..val)
if val == 1 then
    redis.call('expire', key1, expire)
else
    if ttl == -1 then
        redis.call('expire', key1, expire)
    end
end

if val > times then
    return 0
end
return 1
