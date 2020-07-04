package market_maker;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Main {

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Gson g = new Gson();
        String arr = "[\n" +
                "  {\n" +
                "    \"symbol\": \"XBTUSD\",\n" +
                "    \"rootSymbol\": \"XBT\",\n" +
                "    \"state\": \"Open\",\n" +
                "    \"typ\": \"FFWCSX\",\n" +
                "    \"listing\": \"2016-05-04T12:00:00.000Z\",\n" +
                "    \"front\": \"2016-05-04T12:00:00.000Z\",\n" +
                "    \"expiry\": null,\n" +
                "    \"settle\": null,\n" +
                "    \"relistInterval\": null,\n" +
                "    \"inverseLeg\": \"\",\n" +
                "    \"sellLeg\": \"\",\n" +
                "    \"buyLeg\": \"\",\n" +
                "    \"optionStrikePcnt\": null,\n" +
                "    \"optionStrikeRound\": null,\n" +
                "    \"optionStrikePrice\": null,\n" +
                "    \"optionMultiplier\": null,\n" +
                "    \"positionCurrency\": \"USD\",\n" +
                "    \"underlying\": \"XBT\",\n" +
                "    \"quoteCurrency\": \"USD\",\n" +
                "    \"underlyingSymbol\": \"XBT=\",\n" +
                "    \"reference\": \"BMEX\",\n" +
                "    \"referenceSymbol\": \".BXBT\",\n" +
                "    \"calcInterval\": null,\n" +
                "    \"publishInterval\": null,\n" +
                "    \"publishTime\": null,\n" +
                "    \"maxOrderQty\": 10000000,\n" +
                "    \"maxPrice\": 1000000,\n" +
                "    \"lotSize\": 1,\n" +
                "    \"tickSize\": 0.5,\n" +
                "    \"multiplier\": -100000000,\n" +
                "    \"settlCurrency\": \"XBt\",\n" +
                "    \"underlyingToPositionMultiplier\": null,\n" +
                "    \"underlyingToSettleMultiplier\": -100000000,\n" +
                "    \"quoteToSettleMultiplier\": null,\n" +
                "    \"isQuanto\": false,\n" +
                "    \"isInverse\": true,\n" +
                "    \"initMargin\": 0.01,\n" +
                "    \"maintMargin\": 0.004,\n" +
                "    \"riskLimit\": 20000000000,\n" +
                "    \"riskStep\": 10000000000,\n" +
                "    \"limit\": null,\n" +
                "    \"capped\": false,\n" +
                "    \"taxed\": true,\n" +
                "    \"deleverage\": true,\n" +
                "    \"makerFee\": -0.00025,\n" +
                "    \"takerFee\": 0.00075,\n" +
                "    \"settlementFee\": 0,\n" +
                "    \"insuranceFee\": 0,\n" +
                "    \"fundingBaseSymbol\": \".XBTBON8H\",\n" +
                "    \"fundingQuoteSymbol\": \".USDBON8H\",\n" +
                "    \"fundingPremiumSymbol\": \".XBTUSDPI8H\",\n" +
                "    \"fundingTimestamp\": \"2020-07-04T12:00:00.000Z\",\n" +
                "    \"fundingInterval\": \"2000-01-01T08:00:00.000Z\",\n" +
                "    \"fundingRate\": 0.0001,\n" +
                "    \"indicativeFundingRate\": 0.0001,\n" +
                "    \"rebalanceTimestamp\": null,\n" +
                "    \"rebalanceInterval\": null,\n" +
                "    \"openingTimestamp\": \"2020-07-04T11:00:00.000Z\",\n" +
                "    \"closingTimestamp\": \"2020-07-04T12:00:00.000Z\",\n" +
                "    \"sessionInterval\": \"2000-01-01T01:00:00.000Z\",\n" +
                "    \"prevClosePrice\": 9097.84,\n" +
                "    \"limitDownPrice\": null,\n" +
                "    \"limitUpPrice\": null,\n" +
                "    \"bankruptLimitDownPrice\": null,\n" +
                "    \"bankruptLimitUpPrice\": null,\n" +
                "    \"prevTotalVolume\": 134155310054,\n" +
                "    \"totalVolume\": 134155349299,\n" +
                "    \"volume\": 39245,\n" +
                "    \"volume24h\": 4840543,\n" +
                "    \"prevTotalTurnover\": 1907710443532082,\n" +
                "    \"totalTurnover\": 1907710876208207,\n" +
                "    \"turnover\": 432676125,\n" +
                "    \"turnover24h\": 53259081466,\n" +
                "    \"homeNotional24h\": 532.5908146600003,\n" +
                "    \"foreignNotional24h\": 4840543,\n" +
                "    \"prevPrice24h\": 9067.5,\n" +
                "    \"vwap\": 9089.2565,\n" +
                "    \"highPrice\": 9117,\n" +
                "    \"lowPrice\": 9066.5,\n" +
                "    \"lastPrice\": 9070,\n" +
                "    \"lastPriceProtected\": 9070,\n" +
                "    \"lastTickDirection\": \"ZeroMinusTick\",\n" +
                "    \"lastChangePcnt\": 0.0003,\n" +
                "    \"bidPrice\": 9070,\n" +
                "    \"midPrice\": 9070.25,\n" +
                "    \"askPrice\": 9070.5,\n" +
                "    \"impactBidPrice\": 9070,\n" +
                "    \"impactMidPrice\": 9075.5,\n" +
                "    \"impactAskPrice\": 9081.0025,\n" +
                "    \"hasLiquidity\": true,\n" +
                "    \"openInterest\": 69530666,\n" +
                "    \"openValue\": 766297469986,\n" +
                "    \"fairMethod\": \"FundingRate\",\n" +
                "    \"fairBasisRate\": 0.1095,\n" +
                "    \"fairBasis\": 0.08,\n" +
                "    \"fairPrice\": 9073.41,\n" +
                "    \"markMethod\": \"FairPrice\",\n" +
                "    \"markPrice\": 9073.41,\n" +
                "    \"indicativeTaxRate\": 0,\n" +
                "    \"indicativeSettlePrice\": 9073.33,\n" +
                "    \"optionUnderlyingPrice\": null,\n" +
                "    \"settledPrice\": null,\n" +
                "    \"timestamp\": \"2020-07-04T11:17:55.000Z\"\n" +
                "  }\n" +
                "]";

        String err = "{\n" +
                "  \"error\": {\n" +
                "    \"message\": \"Invalid value for argument 'filter' of type 'object'. Received type was string. Error: Unexpected token s in JSON at position 0\",\n" +
                "    \"name\": \"HTTPError\"\n" +
                "  }\n" +
                "}";


        long start = System.currentTimeMillis();

        Class<?> c;
        c = Instrument[].class;
        Instrument[] i = g.fromJson(arr, (Type) c);
        System.out.println(i[0].getSymbol());

        Map<String,Object> params = new HashMap<>();
        params.put("instrument", i[0]);

        Instrument inst = (Instrument) params.get("instrument");
        System.out.println(inst.symbol);

        System.out.println(System.currentTimeMillis() - start);

    }

}



