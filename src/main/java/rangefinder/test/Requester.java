package rangefinder.test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.routecommon.dto.TransitResultCacheDTO;
import com.routecommon.model.transit.Destination;
import com.routecommon.model.transit.Location;
import com.routecommon.model.transit.TransitResponseModel;
import com.routecommon.model.transit.TransitResponsePairModel;
import com.routecommon.model.transit.TransitResult;
import com.routecommon.util.CommonUtil;
import com.routecommon.util.HttpUtil;
import com.routecommon.util.transit.TransitUtil;

import rangefinder.redis.JedisUtils;

public class Requester implements Runnable {
	
	private AtomicInteger pointNumber = new AtomicInteger(0);
	private String taskCertificate;
	private List<Location> targetLocationList;
	private Location startLocation;
	public Requester(String taskCertificate, List<Location> targetLocationList, Location startLocation) { 
		pointNumber.set(0);
		this.taskCertificate = taskCertificate;
		this.targetLocationList = targetLocationList;
		this.startLocation = startLocation;
	}

	@Override
	public void run() {
		while(pointNumber.getAndIncrement() < targetLocationList.size()) {
			int currentNum = pointNumber.get() - 1;
			Location targetLocation = targetLocationList.get(currentNum);
			System.out.println(taskCertificate + ": Visit point pair num : " + pointNumber + "/" + targetLocationList.size());
			TransitResponsePairModel transitResponsePairModel = new TransitResponsePairModel();
			transitResponsePairModel.setFromTransitResponseModel(getTransitResult(startLocation, targetLocation));
			transitResponsePairModel.setToTransitResponseModel(getTransitResult(targetLocation, startLocation));
			if(transitResponsePairModel.isvalidated()) {
				int fromDuration = TransitUtil.getFastRouteDuration(transitResponsePairModel.getFromTransitResponseModel());
				int toDuration = TransitUtil.getFastRouteDuration(transitResponsePairModel.getToTransitResponseModel());
				
				TransitResultCacheDTO transitResultCache = new TransitResultCacheDTO();
				transitResultCache.setTimeFromPos(fromDuration);
				transitResultCache.setTimeToPos(toDuration);
				transitResultCache.setFromLocation(startLocation);
				transitResultCache.setToLocation(targetLocation);
				//mongodb store transitResponsePairModel
				System.out.println(taskCertificate +" 对点 " + currentNum +  "发送");
				JedisUtils.pushListCache(taskCertificate, transitResultCache, true);
				System.out.println(taskCertificate +" 对点 " + currentNum +  "成功");
			} else {
				System.out.println(taskCertificate +" 对点 " + currentNum +  "失败");
			}
		}
	}

	private TransitResponseModel getTransitResult(Location fromLocation, Location toLocation){
		TransitResponseModel transitResponse = null;
		String requestBody="";
		boolean redisSent = false;
		try {
			requestBody = TransitUtil.assembleTransitSchemeRequest(fromLocation, toLocation, "4");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		for(int trytime = 1; trytime < 4; trytime++) {
			String responseJsonString;
			try {
				responseJsonString = HttpUtil.sendGet(requestBody);
			} catch (Exception e) {
				e.printStackTrace();
				if(!requestBody.isEmpty())
					continue;
				else
					break;
			}
			if(responseJsonString != null){
				try {
					transitResponse = CommonUtil.jsonStringToObject(responseJsonString, TransitResponseModel.class);
					if(transitResponse != null){
						//System.out.println("发送");
						if(transitResponse.getStatus() == 1001) {
							Destination destination = new Destination();
							TransitResult transitResult = new TransitResult();
							transitResult.setDestination(destination);
							transitResponse.setTransitResult(transitResult);
						}
						transitResponse.getTransitResult().getDestination().setLocation(toLocation);
						//JedisUtils.pushListCache(taskCertificate, transitResponse, true);
						//System.out.println("成功");
						redisSent = true;
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}

		if(!redisSent){
			//System.out.println("发送错误");
			TransitResponseModel errorTransitResponseModel = new TransitResponseModel();
			TransitResult errorTransitResult = new TransitResult();
			
			Destination errorDestination = new Destination();
			errorDestination.setLocation(toLocation);
			errorTransitResult.setDestination(errorDestination);
			errorTransitResponseModel.setTransitResult(errorTransitResult);
			transitResponse = errorTransitResponseModel;
			//JedisUtils.pushListCache(taskCertificate, errorTransitResponseModel, true);
			//System.out.println("发错误成功");
		}
		return transitResponse;
	}
}