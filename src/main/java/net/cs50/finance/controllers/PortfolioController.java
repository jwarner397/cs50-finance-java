package net.cs50.finance.controllers;

import net.cs50.finance.models.Stock;
import net.cs50.finance.models.StockHolding;
import net.cs50.finance.models.StockLookupException;
import net.cs50.finance.models.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by Chris Bay on 5/17/15.
 */
@Controller
public class PortfolioController extends AbstractFinanceController {

    @RequestMapping(value = "/portfolio")
    public String portfolio(HttpServletRequest request, Model model){

        // Implement portfolio display
        // bring user into scope
        User user = getUserFromSession(request);

        // get user portfolio from table, set into iterable Collection
        Collection<StockHolding> holdings = user.getPortfolio().values();

        // Hashmap to send values to HTML template
        HashMap<String, HashMap<String, String>> printHolding = new HashMap<String, HashMap<String, String>>();

        // iterate over user portfolio and save values into new hashmap
        Stock stock;
        Iterator itr = holdings.iterator();
        while (itr.hasNext()) {
            StockHolding tempHolding = (StockHolding) itr.next();

            // new hashmap to hold the items we want from user portfolio
            HashMap<String, String> newHoldings = new HashMap<String, String>();

            // get current stock information
            try {
                stock = Stock.lookupStock(tempHolding.getSymbol());
                // stock = Stock.toString(stock);
            } catch (StockLookupException e) {
                e.printStackTrace();
                return displayError("Unable to retrieve stock information", model);
            }

            // convert dollar amounts to 2 decimal points
            String currentPrice = String.format("%.2f", stock.getPrice());
            String totalPrice = String.format("%.2f", (stock.getPrice() * (tempHolding.getSharesOwned())));

            // put table values in a hashmap
            newHoldings.put("name", stock.getName());
            newHoldings.put("shares", String.valueOf(tempHolding.getSharesOwned()));
            newHoldings.put("price", currentPrice);
            newHoldings.put("totalValue", totalPrice);
            printHolding.put(stock.getSymbol(), newHoldings);
        }

        String cashOnHand = String.format("%.2f", user.getCash());

        model.addAttribute("title", "Portfolio");
        model.addAttribute("portfolioNavClass", "active");
        model.addAttribute("holdings", printHolding);
        model.addAttribute("cash", cashOnHand);

        return "portfolio";
    }

}
