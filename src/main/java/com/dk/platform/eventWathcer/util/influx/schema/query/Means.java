package com.dk.platform.eventWathcer.util.influx.schema.query;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "elapseTime")
public class Means implements query{
    
    @Column(name = "time")
    public Instant time;
    
    @Column(name = "mean")
    public Double mean;
    
    @Override
    public Double getResult() {
	return mean;
    }
    
    @Override
    public String toString() {
	return String.valueOf(mean);
    }

}
