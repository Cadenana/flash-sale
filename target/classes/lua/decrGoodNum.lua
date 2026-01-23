local stock=tonumber(redis.call("get",KEYS[1]))
if stock<=0 then
    return -1
else
    redis.call("decr",KEYS[1])
    return stock-1
end