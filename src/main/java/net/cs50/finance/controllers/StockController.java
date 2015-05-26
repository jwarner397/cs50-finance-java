package net.cs50.finance.controllers;

import net.cs50.finance.models.Stock;
import net.cs50.finance.models.StockHolding;
import net.cs50.finance.models.StockLookupException;
import net.cs50.finance.models.User;
import net.cs50.finance.models.dao.StockHoldingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Chris Bay on 5/17/15.
 */
@Controller
public class StockController extends AbstractFinanceController {

    @Autowired
    StockHoldingDao stockHoldingDao;

    @RequestMapping(value = "/quote", method = RequestMethod.GET)
    public String quoteForm(Model model) {

        // pass data to template
        model.addAttribute("title", "Quote");
        model.addAttribute("quoteNavClass", "active");
        return "quote_form";
    }

    @RequestMapping(value = "/quote", method = RequestMethod.POST)
    public String quote(String symbol, Model model) throws StockLookupException {

        // Implemented quote lookup
        // initiate Stock variable and call method to look up stock price
        Stock stock;
        try {
            stock = Stock.lookupStock(symbol);
        } catch (StockLookupException e) {
            e.printStackTrace();
            throw new StockLookupException("Unable to look up stock", symbol);
        }

        // format stock price to two decimal places
        String price = String.format("%.2f", stock.getPrice());

        // pass data to template
        model.addAttribute("title", "Quote");
        model.addAttribute("stock_desc", stock.getName());
        model.addAttribute("stock_price", price);
        model.addAttribute("quoteNavClass", "active");

        return "quote_display";
    }

    @RequestMapping(value = "/buy", method = RequestMethod.GET)
    public String buyForm(Model model) {

        model.addAttribute("title", "Buy");
        model.addAttribute("action", "/buy");
        model.addAttribute("buyNavClass", "active");
        return "transaction_form";
    }

    @RequestMapping(value = "/buy", method = RequestMethod.POST)
    public String buy(String symbol, int numberOfShares, HttpServletRequest request, Model model) throws StockLookupException {

        // Implement buy action
        Stock stock;
        StockHolding holding;

        // look up info for stock to buy
        try {
            stock = Stock.lookupStock(symbol);
        } catch (StockLookupException e) {
            e.printStackTrace();
            return displayError("Unable to retrieve stock information", model);
        }

        // get user from session to check and update cash level
        User user = getUserFromSession(request);
        float purchasePrice = stock.getPrice() * numberOfShares;

        // make sure user has enough cash and if so, buy shares
        if (user.getCash() < purchasePrice) {
            return displayError("You do not have enough cash for this purchase", model);
        } else {
            holding = StockHolding.buyShares(user, symbol, numberOfShares);
            // set user cash to new number
            user.setCash(user.getCash() - purchasePrice);
        }

        // update tables with new portfolio and cash information
        stockHoldingDao.save(holding);
        userDao.save(user);

        // send data to HTML template to display successful buy
        model.addAttribute("title", "Buy");
        model.addAttribute("action", "/buy");
        model.addAttribute("buyNavClass", "active");
        model.addAttribute("confirmMessage", "Purchase was successful");

        // display confirmation
        return "transaction_confirm";
    }

    @RequestMapping(value = "/sell", method = RequestMethod.GET)
    public String sellForm(Model model) {
        model.addAttribute("title", "Sell");
        model.addAttribute("action", "/sell");
        model.addAttribute("sellNavClass", "active");
        return "transaction_form";
    }

    @RequestMapping(value = "/sell", method = RequestMethod.POST)
    public String sell(String symbol, int numberOfShares, HttpServletRequest request, Model model) throws StockLookupException {

        // Implement sell action
        Stock stock;
        StockHolding holding;

        // look up info for stock to sell
        try {
            stock = Stock.lookupStock(symbol);
        } catch (StockLookupException e) {
            e.printStackTrace();
            return displayError("Unable to retrieve stock information", model);
        }

        // get user from session to check and update cash level
        User user = getUserFromSession(request);
        float salePrice = stock.getPrice() * numberOfShares;

        // make sure user has enough stock to sell and if so, sell shares
        try{
            // call sellShares method
            StockHolding.sellShares(user, symbol, numberOfShares);
            // set user cash to new number
            user.setCash(user.getCash() + salePrice);
        } catch (IllegalArgumentException e) {
            return displayError("You do not have that many shares to sell", model);
        }

        // update tables with new portfolio and cash information
        userDao.save(user);

        // set variables to display confirmation template
        model.addAttribute("title", "Sell");
        model.addAttribute("action", "/sell");
        model.addAttribute("sellNavClass", "active");
        model.addAttribute("confirmMessage", "Sale of shares successful");

        // display confirmation template
        return "transaction_confirm";
    }

}
