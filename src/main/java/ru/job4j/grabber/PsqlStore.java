package ru.job4j.grabber;


import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            cnn = DriverManager.getConnection(cfg.getProperty("jdbc.url"),
                    cfg.getProperty("jdbc.username"),
                    cfg.getProperty("jdbc.password"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    public static void main(String[] args) {
        try (InputStream in = PsqlStore.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties cfg = new Properties();
            cfg.load(in);
            try (PsqlStore psqlStore = new PsqlStore(cfg)) {
                Post post1 = new Post("title1", "link1", "description1", LocalDateTime.now());
                Post post2 = new Post("title2", "link2", "description2", LocalDateTime.now());
                Post post3 = new Post("title3", "link3", "description3", LocalDateTime.now());
                psqlStore.save(post1);
                psqlStore.save(post2);
                psqlStore.save(post3);
                psqlStore.getAll().forEach(System.out::println);
                System.out.println("__________________________");
                System.out.println(psqlStore.findById(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

        @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                     cnn.prepareStatement("insert into posts(name, text, link, created) values (?, ?, ?, ?)")) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = cnn.prepareStatement("select * from posts order by id")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Post item = getPost(resultSet);
                    posts.add(item);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return posts;
    }

    private Post getPost(ResultSet resultSet) throws SQLException {
        return new Post(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("text"),
                resultSet.getString("link"),
                resultSet.getTimestamp("created").toLocalDateTime()
        );
    }

    @Override
    public Post findById(int id) {
       Post post = null;
       try (PreparedStatement statement = cnn.prepareStatement("select * from posts where id = ?")) {
           statement.setInt(1, id);
           try (ResultSet resultSet = statement.executeQuery()) {
              if (resultSet.next()) {
                  post = getPost(resultSet);
              }
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }
}