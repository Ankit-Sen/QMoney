
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.crio.warmup.stock.portfolio.PortfolioManagerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

    public static RestTemplate restTemplate=new RestTemplate();

    public static PortfolioManager portfolioManager=PortfolioManagerFactory.getPortfolioManager(restTemplate);

    public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
        List<String> result=new ArrayList<>();
        String filePath=args[0];
        String contents=readFileAsString(filePath);
        ObjectMapper om = getObjectMapper();
        PortfolioTrade[] pt = om.readValue(contents, PortfolioTrade[].class);
        for(int i=0;i<pt.length;i++){
            result.add(pt[i].getSymbol());
        }
        System.out.println(result);
        return result;
    }

    public static String readFileAsString(String filename) throws UnsupportedEncodingException, IOException, URISyntaxException{
        return new String(Files.readAllBytes(resolveFileFromResources(filename).toPath()),"UTF-8");
    }

    private static File resolveFileFromResources(String filename) throws URISyntaxException {
        return Paths.get(
                Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
    }


    private static void printJsonObject(Object object) throws IOException {
        Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        logger.info(mapper.writeValueAsString(object));
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static List<String> debugOutputs() {

        String valueOfArgument0 = "trades.json";
        String resultOfResolveFilePathArgs0 = "trades.json";
        String toStringOfObjectMapper = "ObjectMapper";
        String functionNameFromTestFileInStackTrace = "mainReadFile";
        String lineNumberFromTestFileInStackTrace = "";


        return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
                toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
                lineNumberFromTestFileInStackTrace});
    }

    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
        RestTemplate rest = new RestTemplate();
        String Url=prepareUrl(trade,endDate,getToken());
        TiingoCandle[] results= rest.getForObject(Url,TiingoCandle[].class);

        return Arrays.asList(results);
    }

    public static String getToken(){
        String token="2f222c49bd18796cc569704a009d2acbe87abf2b";
        return token;
    }

    public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {
        ObjectMapper om=getObjectMapper();
        String contents=readFileAsString(filename);
        List<PortfolioTrade> trades=Arrays.asList(om.readValue(contents,PortfolioTrade[].class));
        return trades;
    }

    public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

        String Url = "https://api.tiingo.com/tiingo/daily/"+trade.getSymbol()+"/prices?startDate="+trade.getPurchaseDate().toString()+"&endDate="+endDate+"&token="+token;
        return Url;
    }

    static Double getOpeningPriceOnStartDate(List<Candle> candles) {
        return candles.get(0).getOpen();
    }


    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
        return candles.get(candles.size()-1).getClose();
    }

    public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        LocalDate endDate=LocalDate.parse(args[1]);
        List<TotalReturnsDto> tests=new ArrayList<>();
        List<PortfolioTrade> trades=readTradesFromJson(args[0]);
        for(PortfolioTrade t:trades){
            List<Candle> results=fetchCandles(t, endDate, getToken());
            // TiingoCandle[] results=(fetchCandles(t, endDate, getToken())).toArray(new String[0]);
            if(results!=null){
                tests.add(new TotalReturnsDto(t.getSymbol(), results.get(results.size()-1).getClose()));
                //tests.add(new TotalReturnsDto(t.getSymbol(), results[results.length - 1].getClose()));
            }
        }

        Collections.sort(tests,TotalReturnsDto.closingComparator);
        List<String> stocks=new ArrayList<>();
        for(TotalReturnsDto trd : tests){
            stocks.add(trd.getSymbol());
        }
        return stocks;
    }

    public static LocalDate EndDate(String endDate){
        return LocalDate.parse(endDate);
    }




    private static Double GetTimeinYears(LocalDate startDate,LocalDate endDate) {
        Double days=(double)ChronoUnit.DAYS.between(startDate, endDate)/365.24;
        return days;
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
                                                              PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        Double totalReturns=(sellPrice-buyPrice)/buyPrice;
        Double annualizedReturn=Math.pow((1+totalReturns),(1/GetTimeinYears(trade.getPurchaseDate(),endDate)))-1;

        return new AnnualizedReturn(trade.getSymbol(),annualizedReturn,totalReturns);
    }

    public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
            throws IOException, URISyntaxException {
        List<AnnualizedReturn> mAnnualizedReturns=new ArrayList<>();
        List<PortfolioTrade> portfolioTrades=readTradesFromJson(args[0]);
        for(PortfolioTrade trade : portfolioTrades) {
            List<Candle> candles=fetchCandles(trade, EndDate(args[1]), getToken());
            Double buyPrice=candles.get(0).getOpen();
            Double sellPrice=candles.get(candles.size()-1).getClose();
            mAnnualizedReturns.add(calculateAnnualizedReturns(EndDate(args[1]),trade,buyPrice,sellPrice));
        }
        Collections.sort(mAnnualizedReturns,AnnualizedReturn.annualReturnsort);
        return mAnnualizedReturns;
    }

    public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
            throws Exception {
        String file = args[0];
        LocalDate endDate = LocalDate.parse(args[1]);
        String contents = readFileAsString(file);
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] portfolioTrades=objectMapper.readValue(contents,PortfolioTrade[].class);
        PortfolioManager portfolioManager = new PortfolioManagerImpl();
        return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        ThreadContext.put("runId", UUID.randomUUID().toString());

        printJsonObject(mainReadFile(args));
        printJsonObject(mainReadQuotes(args));
        printJsonObject(mainCalculateSingleReturn(args));
        printJsonObject(mainCalculateReturnsAfterRefactor(args));
    }
}

