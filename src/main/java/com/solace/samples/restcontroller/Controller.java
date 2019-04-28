/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.solace.samples.restcontroller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.ConsumerFlowProperties;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.EndpointProperties;
import com.solacesystems.jcsmp.FlowReceiver;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.MapMessage;
import com.solacesystems.jcsmp.Queue;
import com.solacesystems.jcsmp.SDTException;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.StreamMessage;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessage;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Controller {

    private static JCSMPSession session;
    private static XMLMessageProducer prod;


    public static void initializeSession(String... args) throws JCSMPException {
        // Check command line arguments
        if (args.length < 2 || args[1].split("@").length != 2) {
            System.out.println(
                    "Usage: Controller <host:port> <client-username@message-vpn> <client-password> <start-id> <end-id>");
            System.out.println();
            System.exit(-1);
        }
        if (args[1].split("@")[0].isEmpty()) {
            System.out.println("No client-username entered");
            System.out.println();
            System.exit(-1);
        }
        if (args[1].split("@")[1].isEmpty()) {
            System.out.println("No message-vpn entered");
            System.out.println();
            System.exit(-1);
        }

        System.out.println("Controller initializing...");

        // Create a JCSMP Session
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]); // host:port
        properties.setProperty(JCSMPProperties.USERNAME, args[1].split("@")[0]); // client-username
        properties.setProperty(JCSMPProperties.VPN_NAME, args[1].split("@")[1]); // message-vpn
        
//        if (args.length > 2) {
//            properties.setProperty(JCSMPProperties.PASSWORD, args[2]); // client-password
//        }

        session = JCSMPFactory.onlyInstance().createSession(properties);
        session.connect();


    }

    private static void initializeQTransfer(String queueNameSource, String queueNameDest) throws JCSMPException {
    	
    	log.info("Initialied Q Consumer: " + queueNameSource + ":" + queueNameDest) ;
    	
    	
        final Queue queueSource = JCSMPFactory.onlyInstance().createQueue(queueNameSource);
        final Queue queueDest = JCSMPFactory.onlyInstance().createQueue(queueNameDest);

    	
        // Create a Flow be able to bind to and consume messages from the Queue.
        final ConsumerFlowProperties flow_prop = new ConsumerFlowProperties();
        flow_prop.setEndpoint(queueSource);
        flow_prop.setAckMode(JCSMPProperties.SUPPORTED_MESSAGE_ACK_CLIENT);

        EndpointProperties endpoint_props = new EndpointProperties();
        endpoint_props.setAccessType(EndpointProperties.ACCESSTYPE_NONEXCLUSIVE);

        final FlowReceiver cons = session.createFlow(new XMLMessageListener() {
            @Override
            public void onReceive(BytesXMLMessage msg) {
            	
                msg.ackMessage();
                
                BytesMessage newMsg = JCSMPFactory.onlyInstance().createMessage(BytesMessage.class);
                int resCode = 0;
                try {
					resCode = checkAuthorization(msg);
				} catch (SDTException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                
                System.out.printf("BytesMessage: \n");
                byte[] body = msg.getBytes();
                newMsg.setData(body);        
                copyProperties(msg, newMsg);


                if (resCode == 200) {

                	System.out.println("============OLD MESSAGE ===============");
                    System.out.println(msg.dump());
                    try {
    					prod.send(newMsg, queueDest);
    					log.info("Message Transferred");
    	                System.out.println("============NEW MESSAGE ===============");                
    	                System.out.println(newMsg.dump());
    				} catch (JCSMPException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                	
                }
                else {
                	System.out.println("Returning Error ====" + resCode);
                    Destination returnQueue = msg.getReplyTo();
                    
                	System.out.println("Properties==========BEFORE");
                    SDTMap map = newMsg.getProperties();
                    System.out.println(map);
                    try {
                    	map.clear();
						map.putInteger("JMS_Solace_HTTP_status_code", new Integer(resCode));
						map.putString("JMS_Solace_HTTP_field_Server", "SecurityController");
						DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
						df.setTimeZone(TimeZone.getTimeZone("GMT"));
						Date date = new Date();
						map.putString("JMS_Solace_HTTP_field_Date", df.format(date));
                    	
                    	switch(resCode) {
                    		case 401: // Unauthorized
		        						map.putString("JMS_Solace_HTTP_reason_phrase", "Unauthorized");
		        						break;
		        			default:
		        						map.putString("JMS_Solace_HTTP_reason_phrase", "Unknown Error");
		        						break;

                    	}
						newMsg.setProperties(map);
					} catch (SDTException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    
					try {
	                	System.out.println("Properties==========AFTER");
	                    map = newMsg.getProperties();
	                    System.out.println(map);

						prod.send(newMsg, returnQueue);
					} catch (JCSMPException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                }
                

                
   
            }
            
            private int checkAuthorization(BytesXMLMessage msg) throws SDTException {
                System.out.println("============checkAuthorization Start ===============");                

                
                SDTMap map = msg.getProperties();
            	System.out.println(map);
            	String authHeader = map.getString("JMS_Solace_HTTP_field_Authorization");
            	System.out.println("AuthHeader:" + authHeader);
            	if (authHeader == null) {
            		return(401);
            	}
                return(200);
				
			}

			@Override
            public void onException(JCSMPException e) {
                System.out.printf("Consumer received exception: %s%n", e);
            }
        }, flow_prop, endpoint_props);

        // Start the consumer        
        cons.start();
        
    }

    private static void copyProperties(XMLMessage oldMsg, XMLMessage newMsg) {
    	newMsg.setPriority(oldMsg.getPriority());
    	newMsg.setDeliveryMode(oldMsg.getDeliveryMode());
    	newMsg.setProperties(oldMsg.getProperties());
    	newMsg.setDeliveryMode(oldMsg.getDeliveryMode());
    	newMsg.setReplyTo(oldMsg.getReplyTo());
    	newMsg.setAppMessageID(oldMsg.getAppMessageID());
    	newMsg.setSenderId(oldMsg.getSenderId());
    	
    }


//    public static void initializeQProducer(String publishTopic, Queue replyTo) throws JCSMPException {
//        final Topic topic = JCSMPFactory.onlyInstance().createTopic(publishTopic);
//
//        // Time to wait for a reply before timing out
//        final int timeoutMs = 10000;
//        TextMessage request = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
//        final String text = startId + ":" + endId;
//        request.setText(text);
//        request.setReplyTo(replyTo);
//
//
//        prod.send(request,topic);
//
//    }

    public static void initializeQProducer() throws JCSMPException {

        prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            @Override
            public void responseReceived(String messageID) {
                System.out.println("Producer received response for msg: " + messageID);

            }
            @Override
            public void handleError(String messageID, JCSMPException e, long timestamp) {
                System.out.printf("Producer received error for msg: %s@%s - %s%n",
                        messageID,timestamp,e);
            }
        });

    }

    
    public static void setQListener() {
    	log.info("setQListner");
    	
    }

    public static void doIt(String... args) throws JCSMPException, InterruptedException {

        initializeSession(args);
        initializeQProducer();
        initializeQTransfer("pregetQ", "getQ" );
        
        
//        Queue replyTo = session.createTemporaryQueue();
//        initializeConsumer(replyTo);
//        initializeProducer("nse/cache/requests", replyTo);
        Thread.sleep(100000);
        System.out.println("Exiting...");
        session.closeSession();
    }
}
