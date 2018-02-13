package rangefinder.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.routecommon.dto.TransitResultCacheDTO;
import com.routecommon.model.transit.CityRange;
import com.routecommon.model.transit.Location;
import com.routecommon.model.transit.LocationCorrectionParam;
import com.routecommon.model.transit.TransitResponseModel;
import com.routecommon.model.transit.TransitResponsePairModel;
import com.routecommon.util.CommonUtil;
import com.routecommon.util.transit.TransitUtil;

import rangefinder.dto.TransitTaskRequestDTO;
import rangefinder.redis.JedisUtils;

public class SnCal {
	
	
	public static String printErrorPoint(int status, Location lc) {
		String result = "";
		if(status == 0) {
		} else {
			result = "noSolutionPoints.push(new BMap.Point("+lc.getLongitude()+", "+lc.getLatitude()+"));";
		}
		return result;
	}
	
	public static String printPoint(int status, int duration, int expectDuration, Location lc) {
		String result="";
		if(status == 0) {
			if(duration <= expectDuration) {
				result = "reachAblePoints.push(new BMap.Point("+lc.getLongitude()+", "+lc.getLatitude()+"));";
			} else {
				result = "unreachAblePoints.push(new BMap.Point("+lc.getLongitude()+", "+lc.getLatitude()+"));";
			}
			if (duration == Integer.MAX_VALUE){
				result = "noSolutionPoints.push(new BMap.Point("+lc.getLongitude()+", "+lc.getLatitude()+"));";
			} 
		} else {
			result = "errorPoints.push(new BMap.Point("+lc.getLongitude()+", "+lc.getLatitude()+"));";
		}
		return result;
	}
	
	
	public static void  Initialization() throws IOException, TimeoutException {
		//System.out.println("ping Redis: " + JedisUtils.getJedis().ping());
		ConnectionFactory factory = new ConnectionFactory();  
		Channel channel;  
		Connection connection; 
		//hostname of your rabbitmq server  
		factory.setHost("192.168.0.177");  
		factory.setPort(5672);  
		factory.setUsername("guest1");  
		factory.setPassword("guest");  
		factory.setVirtualHost("testmq");
		//getting a connection  
		connection = factory.newConnection();  
		   
		//creating a channel  
		channel = connection.createChannel();  
		   
		//declaring a queue for this channel. If queue does not exist,  
		//it will be created on the server.  
		channel.queueDeclare("huader.key", true, false, false, null);  
		 
		TaskQueueConsumer consumer = new TaskQueueConsumer();
		consumer.channel = channel;
		Thread consumerThread = new Thread(consumer);  
		consumerThread.start();
		
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10);
		for(int k= 0 ; k < 10; ++k)
			scheduledThreadPool.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						try {
							TransitTaskRequestDTO transitTaskRequest = TaskQueueConsumer.transitTaskQueue.poll(500, TimeUnit.MILLISECONDS);
							if(transitTaskRequest != null) {
								String taskCertificate = transitTaskRequest.getTaskCertificate();
								System.out.println("开始处理  " + taskCertificate + "任务");
								
								JedisUtils.putCache(taskCertificate +"FromPos", transitTaskRequest.getTransitRequestDTO().getETAfromPos());
								JedisUtils.putCache(taskCertificate +"ToPos", transitTaskRequest.getTransitRequestDTO().getETAToPos());
								LocationCorrectionParam locationCorrectionParam = new LocationCorrectionParam(transitTaskRequest.getTransitRequestDTO().getBlockSizeFactor());
								CityRange cityRange = TransitUtil.getNearestCityRange(transitTaskRequest.getTransitRequestDTO().getPosition());
								List<Location> allLocationList = TransitUtil.getDestinationLocationList(cityRange, locationCorrectionParam);
								Location oriApproxiLocation = TransitUtil.getNearestLocation(allLocationList, transitTaskRequest.getTransitRequestDTO().getPosition());
								System.out.print("taskCertificate:" + taskCertificate + " Total points : " + allLocationList.size());
								System.out.println("   Block Size : " + locationCorrectionParam.getBlockSize());
								
								Requester requester = new Requester(taskCertificate, allLocationList, oriApproxiLocation);
								ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
								for (int i = 1; i <= 80; ++i) {
									cachedThreadPool.execute(requester);
								}
								cachedThreadPool.shutdown();
								while(!cachedThreadPool.isTerminated());
								TransitResultCacheDTO finish = new TransitResultCacheDTO();
								finish.setFinish(true);
								JedisUtils.pushListCache(taskCertificate, finish, true);
								System.out.println(taskCertificate +"发送结束标志成功");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}, 1, 1, TimeUnit.SECONDS
			);
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, TimeoutException  {
		Initialization();
		System.out.println("后端初始化完成");
		Double blockSizeFactor = 10.0;
		int expectDuration = 1000;
		while(true) {
			try{
				Scanner cin = new Scanner(System.in);
				blockSizeFactor = cin.nextDouble();
				expectDuration = cin.nextInt();
				
			} catch (Throwable t){
				System.out.println("输入错误，请重输");
				continue;
			}
			if(blockSizeFactor > 1666.666) {
				System.out.println("退出");
				break;
			}
			System.out.println("========================================================================================================");
		}
			/*CityRange cityRange = new CityRange(new Location(30.392, 120.04879),  new Location(30.097, 120.39086));//new CityRange(new Location(30.392, 120.04879),  new Location(30.097, 120.39086));
			LocationCorrectionParam locationCorrectionParam = new LocationCorrectionParam(blockSizeFactor);
			List<Location> allLocationList = getDestinationLocationList(cityRange, locationCorrectionParam);
			
			//Location oriLocation = new Location(30.193628, 120.186317);
			Location oriApproxiLocation = getNearestLocation(allLocationList, oriLocation);
			oos = new ObjectOutputStream(new FileOutputStream(oriApproxiLocation.getLongitude() +"," + oriApproxiLocation.getLatitude()+".txt"));
			//double latitudeLenth = northWestCorner.getLatitude() - southEestCorner.getLatitude();
			//double longitudeLenth = southEestCorner.getLongitude() - northWestCorner.getLongitude();
			
			System.out.print("Total points : " + allLocationList.size());
			System.out.println("   Block Size : " + locationCorrectionParam.getBlockSize());
			List<String> resultList = new ArrayList<>();
			Map<Location, TransitResponseModel> transitCache = cache.get(blockSizeFactor);
			if(transitCache == null) {
				transitCache = new HashMap<>();
			}
			int pointNumber = 1;
			
			for(Location destLocation : allLocationList) {
				System.out.print("Visit point num : " + pointNumber + "/" + allLocationList.size());
				int duration = Integer.MAX_VALUE;
				
				TransitResponseModel transitResponse = transitCache.get(destLocation);
				if(transitResponse != null) {
					System.out.println(" from cache, "+ (allLocationList.size() - pointNumber) + " remaining");
					duration = getFastRouteDuration(transitResponse);
					pointNumber++;
				} else {
					System.out.println(" from remote service, "+ (allLocationList.size() - pointNumber) + " remaining");
					pointNumber++;
					String request;
					try {
						request = assembleTransitSchemeRequest(oriApproxiLocation, destLocation, "4");
					} catch (UnsupportedEncodingException e1) {
						e1.printStackTrace();
						resultList.add(printErrorPoint(1, destLocation));
						continue;
					}
					for(int trytime = 1; trytime < 4; trytime++){
						String responseJsonString;
						
						try {
							responseJsonString = HttpUtil.sendGet(request);
						} catch (Exception e) {
							e.printStackTrace();
							resultList.add(printErrorPoint(2, destLocation));
							continue;
						}
						try {
							transitResponse = CommonUtil.jsonStringToObject(responseJsonString, TransitResponseModel.class);
						} catch (Exception e) {
							e.printStackTrace();
							resultList.add(printErrorPoint(3, destLocation));
							continue;
						}
						duration = getFastRouteDuration(transitResponse);
						if(duration!=Integer.MAX_VALUE) {
							transitCache.put(destLocation, transitResponse);
							break;
						}
						if (trytime==3) {
							transitCache.put(destLocation, transitResponse);
					    	break;
					    	//System.out.println("\n请求失败："+responseJsonString);
					    	//return ;
					    }
					}
				}
				resultList.add(printPoint(0, duration, expectDuration, destLocation));
			}
			
			cache.put(blockSizeFactor, transitCache);
			System.out.println("Total points : " + allLocationList.size());
			System.out.println("============================================================================================");
			for(String result : resultList) {
				if(!result.isEmpty())
					System.out.println(result);
			}
			System.out.println("MainPoints.push(new BMap.Point("+cityRange.getCityCenter().getLongitude()+", "+cityRange.getCityCenter().getLatitude()+"));");
			System.out.println("MainPoints.push(new BMap.Point("+oriLocation.getLongitude()+", "+oriLocation.getLatitude()+"));");
			System.out.println("MainPoints.push(new BMap.Point("+oriApproxiLocation.getLongitude()+", "+oriApproxiLocation.getLatitude()+"));");

			System.out.println("============================================================================================");
			System.out.print("blockSizeFactor: " + blockSizeFactor);
			System.out.println("    expectDuration: " + expectDuration);
			System.out.println("============================================================================================");
		}
		return oos;*/
	}

	
	
}
