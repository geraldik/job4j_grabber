package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";

    private static final String PAGE_LINK = String.format("%s/vacancies/java_developer", SOURCE_LINK);

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public static void main(String[] args) {
        var habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> posts = habrCareerParse.list(PAGE_LINK);
        posts.forEach(System.out::println);
    }

    private static String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements description = document.getElementsByClass("style-ugc");
        return description.text();
    }

    private Post getPost(Element row) {
        Element titleElement = row.select(".vacancy-card__title").first();
        Element linkElement = titleElement.child(0);
        Element timeElement = row.select(".vacancy-card__date").first().child(0);
        String vacancyName = titleElement.text();
        String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
        return new Post(vacancyName, link, retrieveDescription(link),
                dateTimeParser.parse(timeElement.attr("datetime")));
    }

    @Override
    public List<Post> list(String link) {
        List<Post> posts = new ArrayList<>(150);
        for (int i = 1; i < 6; i++) {
            String page = String.format("%s%s%d", link, "?page=", i);
            Connection connection = Jsoup.connect(page);
            Document document = null;
            try {
                document = connection.get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> posts.add(getPost(row)));
        }

        return posts;
    }
}