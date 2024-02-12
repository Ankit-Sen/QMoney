
package com.crio.warmup.stock.quotes;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;


public class AlphavantageService implements StockQuotesService {

    private RestTemplate restTemplate;
    public static final String FUNCTION="TIME_SERIES_DAILY";

    protected AlphavantageService(RestTemplate restTemplate){
        this.restTemplate=restTemplate;
    }



    @Override
    public List<Candle> getStockQuote(String symbol, LocalDate startDate, LocalDate endDate)
            throws JsonProcessingException, StockQuoteServiceException, RuntimeException
    {
        //CHECKSTYLE:ON
        try {
            String response = restTemplate.getForObject(buildUri(symbol), String.class);
            System.out.println(response+"ERROR  ERROR");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            AlphavantageDailyResponse result =
                    objectMapper.readValue(response, AlphavantageDailyResponse.class);
            return result.getCandles().entrySet().stream()
                    .filter(entry -> between(entry.getKey(), startDate, endDate))
                    .map(entry -> {
                        try {
                            entry.getValue().setDate(entry.getKey());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return entry.getValue();
                    })
                    .sorted(Comparator.comparing(Candle::getDate))
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {


            throw new StockQuoteServiceException("Alphavantage returned invalid Response",e);
        }
    }

    private boolean between(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return startDate.atStartOfDay().minus(1, SECONDS).isBefore(date.atStartOfDay())
                && endDate.plus(1, DAYS).atStartOfDay().isAfter(date.atStartOfDay());
    }


    protected static String buildUri(String symbol) {
        String Url = "https://www.alphavantage.co/query?function="+FUNCTION+"&symbol="+symbol+"&output=full&apikey="+getToken();
        return Url;
    }

    public static String getToken(){
        String token="3D14G74F78ESZW4I";
        return token;
    }

}

