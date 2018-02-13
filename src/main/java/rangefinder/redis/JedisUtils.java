package rangefinder.redis;

import com.routecommon.util.SerializerUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisUtils {

	private static JedisPool pool;
	static {
		// 建立连接池配置参数
		JedisPoolConfig config = new JedisPoolConfig();
		// 设置最大连接数
		config.setMaxTotal(200);
		// 设置最大阻塞时间，记住是毫秒数milliseconds
		config.setMaxWaitMillis(100000);
		// 设置空间连接
		config.setMaxIdle(50);
		// 创建连接池
		pool = new JedisPool(config, "192.168.0.177", 6379, 100000);
	}

	private static Jedis getJedis() {
		return pool.getResource();
	}


	public static void removeCache(String key) {
		Jedis jedis = getJedis();
		try{
			jedis.del(key.getBytes());
		} finally {
			jedis.close();
		}
	}
	
	public static <T> String putCache(String key, T obj) {
		final byte[] bkey = key.getBytes();
		final byte[] bvalue = SerializerUtil.serializeObj(obj);
		Jedis jedis = getJedis();
		try{
			jedis.expire(bkey, 5000);
			return jedis.set(bkey, bvalue);
			
		} finally {
			jedis.close();
		}
	}

	//根据key取缓存数据
	public static <T> T getCache(final String key) {
		byte[] result; 
		Jedis jedis =getJedis();
		try{
			result = jedis.get(key.getBytes());
		} finally {
			jedis.close();
		}
		return (T) SerializerUtil.deserializeObj(result);
	}
	
	public static <T> T getListCache(String key, boolean fromR) {
		byte[] result; 
		Jedis jedis =getJedis();
		try{
			if(fromR)
				result = jedis.lpop(key.getBytes());
			else
				result = jedis.rpop(key.getBytes());
		} finally {
			jedis.close();
		}
		return (T) SerializerUtil.deserializeObj(result);
	}

	
	public static <T> Long pushListCache(String key, T obj, boolean fromR) {
		byte[] result; 
		Jedis jedis =getJedis();
		try{
			Long l;
			if(fromR)
				l = jedis.rpush(key.getBytes(),SerializerUtil.serializeObj(obj));
			else
				l = jedis.lpush(key.getBytes(),SerializerUtil.serializeObj(obj));
			jedis.expire(key.getBytes(),5000);
			return l;
		} finally {
			jedis.close();
		}
	}
}