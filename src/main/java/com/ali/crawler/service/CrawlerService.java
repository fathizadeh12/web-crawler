package com.ali.crawler.service;

import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ali.crawler.entity.Product;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;

import java.util.stream.Collectors;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Service
public class CrawlerService {


    private final TaskExecutor taskExecutor;

    /*
    I used jdbcTemplate because spring boot integration with hibernate (JPA) doesn't support the SQLite
     */
    private final JdbcTemplate jdbcTemplate;


    List<Product> allProductList = new ArrayList<>();

    public CrawlerService(JdbcTemplate jdbcTemplate, TaskExecutor taskExecutor) {
        createDatabaseAndTable(jdbcTemplate);
        this.taskExecutor = taskExecutor;
        this.jdbcTemplate = jdbcTemplate;
    }

    void createDatabaseAndTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP TABLE products");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS products(p_name varchar(100), price varchar(100),detail varchar(8000))");
    }

    public void fetchProductsCategoriesLinks(String url) throws IOException {

        Document duc = Jsoup.connect(url).get();
        Elements productLinks = duc.select("li.level-top").select("a[href].level-top");
        for (Element element : productLinks) {
            taskExecutor.execute(() -> {
                try {
                    fetchTopAndBottomCategoriesLink(element.attr("abs:href"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        }
    }


    private void fetchTopAndBottomCategoriesLink(String url) throws IOException {
        Document duc = Jsoup.connect(url).get();
        Elements categories = duc.select("ol.items").select("li.item").select("a[href]");
        for (Element element : categories) {
            System.out.println(element.attr("abs:href"));

            taskExecutor.execute(() -> {
                try {
                    fetchProductInfoPageLinks(element.attr("abs:href"));
                    fetchProductsLinks(element.attr("abs:href"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


        }
    }

    private void fetchProductsLinks(String url) throws IOException {
        Document duc = Jsoup.connect(url).get();
        Elements pages = duc.select("div.toolbar-products");
        if (pages.size() != 0) {
            pages = pages.get(0).select("div.pages").select("li.item").select("a.page").select("a[href]");
        }
        for (Element element : pages) {
            System.out.println(element.attr("abs:href"));
            taskExecutor.execute(() -> {
                try {
                    fetchProductInfoPageLinks(element.attr("abs:href"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void fetchProductInfoPageLinks(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Elements products = doc.select("li.product.item").select("a.product-item-photo").select("a[href]");
        for (Element pro : products) {
            taskExecutor.execute(() -> {
                try {
                    fetchProductInfo(pro.attr("abs:href"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private void fetchProductInfo(String URL) throws IOException {

        Document document = Jsoup.connect(URL).get();
        String product = document.select("span.base").text();
        Elements pricesEls = document.select("span.price");
        String detail = document.select("div.description").select("p").text();

        String price = null;
        for (Element el : pricesEls) {
            price = el.text();
            break;
        }

        Product productObj = new Product(product, price, detail);
        if (allProductList.stream().filter(p -> p.getName().equals(productObj.getName())).collect(Collectors.toList()).size() == 0) {

            allProductList.add(productObj);
            showProductsInfo(productObj);
            persistProductToDatabase(productObj);
        }

    }


    private void persistProductToDatabase(Product product) {
        jdbcTemplate.update("INSERT INTO products VALUES (?,?,?)", product.getName(), product.getPrice(), product.getDetail());

    }


    private void showProductsInfo(Product product) {

        System.out.println("product name: " + product.getName());
        System.out.println("product price: " + product.getPrice());
        System.out.println("product detail: " + product.getDetail());
        System.out.println("_____________________________________________");


    }

}
