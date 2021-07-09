package com.dk.platform.eventWathcer.util.influx.schema.point;

import com.dk.platform.eventWathcer.util.influx.InfluxDbConn;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class MessageSchema {
    
    private Builder ElapseBuiler  = null;
    
    private String ElapseMeasurement = null; 
    
    private Builder MsgCountBuilder = null;
    
    private String MsgCountMeasurement = null;
    
    private Builder MessageBuilder = null;
    
    private String MessageMeasurement = null;
    
    
    public MessageSchema() {
	this.ElapseMeasurement = "elapse";
	this.ElapseBuiler = Point.measurement(ElapseMeasurement).tag("process", "value").addField("value", 10);
	
	this.MsgCountMeasurement = "msgCount";
	this.MsgCountBuilder = Point.measurement(MsgCountMeasurement).tag("queue", "value").addField("value", 10);
	
	// TODO MessaegBuilder.
	this.MessageMeasurement = "message";
	this.MessageBuilder = Point.measurement(MessageMeasurement);
	
	
    }
    
//    public Point generateElapsePoint(String tagValue) {
////	this.ElapseBuiler.tag("process", tagValue).addField("value", false)
//    }
    
    public Builder getElapseBuilder() {
	return this.ElapseBuiler;
    }
    
    public Builder getMsgCountBuilder() {
	return this.MsgCountBuilder;
    }
    
    void usage() {
	
	// 1. get Builder
	Builder buidler = this.getElapseBuilder();
	
	// 2. Set Tag and Field. Set Same Key
	buidler.tag("process", "test");
	buidler.addField("value", 20);
	
	// 3.Set TIme
	buidler.time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
	
	//4. Make it Point
	Point point = buidler.build();
	
    }
}

class SamplePoint {
    
    /**
     * =======> Write Single Point Sample <=========
     *Point point = Point.measurement("memory")
  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
  .addField("name", "server1")
  .addField("free", 4743656L)
  .addField("used", 1015096L)
  .addField("buffer", 1010467L)
  .build();
     * */
    
    private Builder builder = Point.measurement("as").tag("dest", "test").addField("value", 15);

    public SamplePoint() {
	
	// Can I Re-Use Point???
	
	/**
	 * Builder�� �����, schema���� �̸� ���Ǹ� �س�
	 * tag , key key �� ��������.  �� => schema�� �����ҰŴϱ�
	 * ���ϴ°� value�ε�.  value�� parameter ��������
	 * ����)  add file or add tag�� �ߺ����� �ϸ� value�� ����Ⱑ �ɰŴ� =>  override
	 * ���)  �׽�Ʈ�� �غ���  
	 */
//	Builder builder = Point.measurement("emslog");
	
	// set - time
	// Long  & TimeUnit
	this.builder.time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
	
	// Change field value.  by adding on
	this.builder.addField("value", 20);	// => OverWrite
	this.builder.tag("dest", "test1");		// => OverWrite
		
	Point point = this.builder.build();
	
	InfluxDbConn db = InfluxDbConn.getInstance();
	InfluxDB dbClient = db.getInfluxDB();
	dbClient.write(point);
	
	
	
    }
    
    private Builder GenerateBuilder(String measure) {
	
	Builder builder = Point.measurement(measure).tag("dest", "test").addField("value", 10);
	return builder;
	
    }
    
    
    public static void main(String[] args) {
	new SamplePoint();
    }
}


