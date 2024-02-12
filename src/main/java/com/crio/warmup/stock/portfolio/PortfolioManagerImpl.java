
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

    private StockQuotesService stockQuotesService;
    private RestTemplate restTemplate;
    protected PortfolioManagerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    public PortfolioManagerImpl() {}

    public PortfolioManagerImpl(StockQuotesService stockQuotesService){
        this.stockQuotesService=stockQuotesService;

    }

    public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)throws JsonProcessingException, StockQuoteServiceException {

        return stockQuotesService.getStockQuote(symbol, from, to);
    }


    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
                                                            LocalDate endDate) throws  StockQuoteServiceException{
        // TODO Auto-generated method stub
        List<AnnualizedReturn> mAnnualizedReturns=new ArrayList<>();
        for(PortfolioTrade trade : portfolioTrades) {
            List<Candle> candles;
            try {
                candles = getStockQuote(trade.getSymbol(),trade.getPurchaseDate(), endDate);
                Double buyPrice=candles.get(0).getOpen();
                Double sellPrice=candles.get(candles.size()-1).getClose();
                mAnnualizedReturns.add(calculateAnnualizedReturns(endDate,trade,buyPrice,sellPrice));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


        }
        Collections.sort(mAnnualizedReturns,AnnualizedReturn.annualReturnsort);
        return mAnnualizedReturns;
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


    public AnnualizedReturn getAnnualizedAndTotalReturns(PortfolioTrade trade, LocalDate endDate)
            throws StockQuoteServiceException {
        LocalDate startDate = trade.getPurchaseDate();
        String symbol = trade.getSymbol();


        Double buyPrice = 0.0, sellPrice = 0.0;


        try {
            LocalDate startLocalDate = trade.getPurchaseDate();

            List<Candle> stocksStartToEndFull = getStockQuote(symbol, startLocalDate, endDate);


            Collections.sort(stocksStartToEndFull, (candle1, candle2) -> {
                return candle1.getDate().compareTo(candle2.getDate());
            });

            Candle stockStartDate = stocksStartToEndFull.get(0);
            Candle stocksLatest = stocksStartToEndFull.get(stocksStartToEndFull.size() - 1);


            buyPrice = stockStartDate.getOpen();
            sellPrice = stocksLatest.getClose();
            endDate = stocksLatest.getDate();


        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
        Double totalReturn = (sellPrice - buyPrice) / buyPrice;


        long daysBetweenPurchaseAndSelling = ChronoUnit.DAYS.between(startDate, endDate);
        Double totalYears = (double) (daysBetweenPurchaseAndSelling) / 365;


        Double annualizedReturn = Math.pow((1 + totalReturn), (1 / totalYears)) - 1;
        return new AnnualizedReturn(symbol, annualizedReturn, totalReturn);


    }


    @Override
    public List<AnnualizedReturn> calculateAnnualizedReturnParallel (List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads) throws StockQuoteServiceException ,
            RuntimeException, InterruptedException {
        List<AnnualizedReturn> annualizedReturns =new ArrayList<AnnualizedReturn>();
        List<Future<AnnualizedReturn>> futureReturnsList = new ArrayList<Future<AnnualizedReturn>>();
        final ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < portfolioTrades.size(); i++) {
            PortfolioTrade trade = portfolioTrades.get(i);
            Callable<AnnualizedReturn> callableTask = () -> {
                return getAnnualizedAndTotalReturns(trade, endDate);
            };

            Future<AnnualizedReturn> futureReturns = pool.submit(callableTask);
            futureReturnsList.add(futureReturns);
        }

        for (int i = 0; i < portfolioTrades.size(); i++) {
            Future<AnnualizedReturn> futureReturns = futureReturnsList.get(i);
            try {
                AnnualizedReturn returns = futureReturns.get();
                annualizedReturns.add(returns);
            } catch (ExecutionException e) {
                throw new StockQuoteServiceException("Error when calling the API", e);

            }
        }
        Collections.sort(annualizedReturns, AnnualizedReturn.annualReturnsort);
        return annualizedReturns;

    }

}
