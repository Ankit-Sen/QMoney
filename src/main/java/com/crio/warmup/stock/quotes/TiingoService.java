
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

    private RestTemplate restTemplate;


    protected TiingoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
            throws JsonProcessingException, StockQuoteServiceException {
        // TODO Auto-generated method stub
        List<Candle> stocksStartToEnd=new ArrayList<>();
        if(from.compareTo(to) >= 0)
            throw new RuntimeException();
        try{
            String Url=buildUri(symbol,from,to);
            String stocks= restTemplate.getForObject(Url,String.class);
            ObjectMapper objectMapper=new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            TiingoCandle[] results=objectMapper.readValue(stocks, TiingoCandle[].class);
            // System.out.println("Open Price : "+results[0].getOpen());
            // System.out.println("Open Price : "+results[results.length-1].getOpen());
            stocksStartToEnd=Arrays.asList(results);

        }catch (NullPointerException e) {

            throw new StockQuoteServiceException("Tiingo API returned invalid Response",e.getCause());
        }
        return stocksStartToEnd;
    }


    protected static String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
        String Url = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate+"&endDate="+endDate+"&token="+getToken();
        return Url;
    }

    public static String getToken(){
        String token="2f222c49bd18796cc569704a009d2acbe87abf2b";
        return token;
    }

}
