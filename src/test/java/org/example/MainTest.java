package org.example;

import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.example.entity.Child;
import org.example.entity.Parent;
import org.example.entity.Parent_;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.graph.RootGraph;
import org.hibernate.jpa.SpecHints;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static org.hibernate.boot.SchemaAutoTooling.CREATE;
import static org.hibernate.boot.SchemaAutoTooling.VALIDATE;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.JdbcSettings.HIGHLIGHT_SQL;
import static org.hibernate.cfg.JdbcSettings.PASS;
import static org.hibernate.cfg.JdbcSettings.SHOW_SQL;
import static org.hibernate.cfg.JdbcSettings.URL;
import static org.hibernate.cfg.JdbcSettings.USER;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class MainTest {

    @Container
    private static PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:12.19")
            .withDatabaseName("example")
            .withUsername("user")
            .withPassword("1");

    SessionFactory createSessionFactory(boolean createSchema) {
        return new Configuration()
                .addAnnotatedClass(Parent.class)
                .addAnnotatedClass(Child.class)
                .setProperty(URL, postgresqlContainer.getJdbcUrl())
                .setProperty(USER, postgresqlContainer.getUsername())
                .setProperty(PASS, postgresqlContainer.getPassword())
                .setProperty(JAKARTA_HBM2DDL_DATABASE_ACTION, createSchema ? CREATE : VALIDATE)
                .setProperty("hibernate.agroal.maxSize", "20")
                .setProperty(SHOW_SQL, TRUE.toString())
                .setProperty(FORMAT_SQL, TRUE.toString())
                .setProperty(HIGHLIGHT_SQL, TRUE.toString())
                .buildSessionFactory();
    }

    private RootGraph<Parent> parentWithChildGraph(Session session) {
        RootGraph<Parent> entityGraph = session.createEntityGraph(Parent.class);
        entityGraph.addSubgraph( Parent_.children);
        return entityGraph;
    }

    @Test
    void testGraphNotWorkingWith2SessionFactories() {
        try (SessionFactory factory1 = createSessionFactory(true);
             SessionFactory factory2 = createSessionFactory(false);
             Session session = factory1.openSession()) {

            createData(session);

            selectAndAssert(session);
        }
    }

    @Test
    void testGraphWorkingWith1SessionFactories() {
        try (SessionFactory factory1 = createSessionFactory(true);
             Session session = factory1.openSession()) {

            createData(session);

            selectAndAssert(session);
        }
    }

    private void selectAndAssert(Session session) {
        List<Parent> resultList = session.createQuery("select p from Parent p join p.children c where c.name = :name", Parent.class)
                .setParameter("name", "child1")
                .setHint(SpecHints.HINT_SPEC_FETCH_GRAPH, parentWithChildGraph(session))
                .getResultList();
        assertEquals(resultList.size(), 1);
        Parent parent = resultList.getFirst();
        assertTrue(Hibernate.isInitialized(parent.getChildren()));
    }

    private void createData(Session session) {
        session.beginTransaction();
        Child child1 = new Child();
        child1.setName("child1");
        session.persist(child1);
        Child child2 = new Child();
        child2.setName("child1");
        session.persist(child2);
        Parent p = new Parent();
        p.setName("parent");
        p.setChildren(Set.of(child1, child2));
        session.persist(p);
        child1.setParent(p);
        child2.setParent(p);
        session.flush();
        session.clear();
        session.getTransaction().commit();
    }


}