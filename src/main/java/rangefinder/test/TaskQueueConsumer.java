package rangefinder.test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import com.routecommon.dto.TransitRequestDTO;
import com.routecommon.util.CommonUtil;

import rangefinder.dto.TransitTaskRequestDTO;

public class TaskQueueConsumer implements Runnable, Consumer{
	public Channel channel;  
	public static BlockingQueue<TransitTaskRequestDTO> transitTaskQueue = new LinkedBlockingQueue<TransitTaskRequestDTO>(100);  
	@Override
	public void run() {
		// TODO Auto-generated method stub
		 try {
			channel.basicConsume("huader.key", true,this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public void handleCancel(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleCancelOk(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleConsumeOk(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleDelivery(String arg0, Envelope arg1, BasicProperties arg2, byte[] arg3) throws IOException {
		String taskInstruction = new String(arg3, "UTF-8");
		if(taskInstruction != null) {
			taskInstruction = taskInstruction.replace("\\\"", "\"");
			if(taskInstruction.length() > 9){
				String taskCertificate = taskInstruction.substring(1, 9);
				String taskJsonString = taskInstruction.substring(9);
				
				TransitRequestDTO transitRequest = CommonUtil.jsonStringToObject(taskJsonString, TransitRequestDTO.class);
				if(transitRequest != null) {
					TransitTaskRequestDTO transitTask = new TransitTaskRequestDTO();
					transitTask.setTaskCertificate(taskCertificate);
					transitTask.setTransitRequestDTO(transitRequest);
					try {
						if(transitTaskQueue.offer(transitTask, 1, TimeUnit.SECONDS)) {
							System.out.println("Enqueued Task:  " + taskInstruction);
						} else {
							System.out.println("Failed enqueued Task:  " + taskInstruction);
						}
						return;
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.out.println("Failed enqueued Task:  " + taskInstruction);
						return;
					}
					
				}	
			}
				
		}
	}

	@Override
	public void handleRecoverOk(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleShutdownSignal(String arg0, ShutdownSignalException arg1) {
		// TODO Auto-generated method stub
		
	}

}
