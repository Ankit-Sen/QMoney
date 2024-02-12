package com.crio.warmup.stock.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphavantageCandle implements Candle {
    @JsonProperty("1. open")
    private Double open;
    @JsonProperty("2. high")
    private Double high;
    @JsonProperty("3. low")
    private Double low;
    @JsonProperty("4. close")
    private Double close;


    private LocalDate date;

    @Override
    public Double getOpen() {
        // TODO Auto-generated method stub
        return this.open;
    }

    @Override
    public Double getClose() {
        // TODO Auto-generated method stub
        return this.close;
    }

    @Override
    public Double getHigh() {
        // TODO Auto-generated method stub
        return this.high;
    }

    @Override
    public Double getLow() {
        // TODO Auto-generated method stub
        return this.low;
    }

    @Override
    public LocalDate getDate() {
        // TODO Auto-generated method stub
        return this.date;
    }

    public void setDate(LocalDate date2) {
        this.date=date2;
    }

    public void setOpen(Double open) {
        // TODO Auto-generated method stub
        this.open=open;
    }

    public void setClose(double close) {
        // TODO Auto-generated method stub
        this.close=close;
    }

    public void setHigh(Double high) {
        // TODO Auto-generated method stub
        this.high=high;
    }

    public void setLow(Double low) {
        // TODO Auto-generated method stub
        this.low=low;
    }

}
