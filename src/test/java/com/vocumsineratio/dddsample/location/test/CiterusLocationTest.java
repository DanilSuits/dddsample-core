/**
 * Copyright Vast 2018. All Rights Reserved.
 * <p/>
 * http://www.vast.com
 */
package com.vocumsineratio.dddsample.location.test;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.SampleLocations;
import se.citerus.dddsample.infrastructure.persistence.hibernate.LocationRepositoryHibernate;

import java.util.List;

/**
 * @author Danil Suits (danil@vast.com)
 */
public class CiterusLocationTest {
    /*
        This test is primarily a discovery exercise to understand how the
        Hibernate configuration sequence works when you wire it up "by hand"
        without the help of Spring annotations.
     */
    @Test
    public void configureSessionFactory() {
        // Because saving the Location modifies the in memory instance
        // (using reflection to change the id), we get weird effects
        // if we try to re-use a static variable.
        Location whereShouldWeBe = SampleLocations.STOCKHOLM;
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
        // Use the URL to ensure we have an in memory database instance
        // used by this test alone.
        dataSource.setUrl("jdbc:hsqldb:mem:leftHandedTest");
        dataSource.setUsername("sa");
        dataSource.setInitialSize(4);
        dataSource.setDefaultAutoCommit(false);

        LocalSessionFactoryBuilder sessionFactoryBuilder = new LocalSessionFactoryBuilder(dataSource);
        sessionFactoryBuilder.configure("hibernate.cfg.xml");

        SessionFactory sessionFactory = sessionFactoryBuilder.buildSessionFactory();

        LocationRepositoryHibernate repo = new LocationRepositoryHibernate();
        repo.setSessionFactory(sessionFactory);

        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        TransactionTemplate tt = new TransactionTemplate(transactionManager);


        tt.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        sessionFactory
                                .getCurrentSession()
                                .saveOrUpdate(whereShouldWeBe);
                    }
                }
        );


        List<Location> locations = tt.execute(
                new TransactionCallback<List<Location>>() {
                    @Override
                    public List<Location> doInTransaction(TransactionStatus transactionStatus) {
                        return repo.findAll();
                    }
                }
        );

        Assert.assertEquals(1, locations.size());

        Location whereAreWe = tt.execute(
                new TransactionCallback<Location>() {
                    @Override
                    public Location doInTransaction(TransactionStatus transactionStatus) {
                        return repo.find(whereShouldWeBe.unLocode());
                    }
                }
        );

        Assert.assertEquals(whereShouldWeBe.unLocode().idString(), whereAreWe.unLocode().idString());
        Assert.assertEquals(whereShouldWeBe.name(), whereAreWe.name());
    }

    /*
        This test is primarily a discovery exercise to understand how to wire up a single
        Hibernate entity.
     */
    @Test
    public void rightHandedTest() {
        // Because saving the Location modifies the in memory instance
        // (using reflection to change the id), we get weird effects
        // if we try to re-use a static variable.
        Location whereShouldWeBe = SampleLocations.HONGKONG;
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.hsqldb.jdbcDriver");

        // Use the URL to ensure we have an in memory database instance
        // used by this test alone.  DONT CHEAT THE TEST by re-using
        // an database instance that may have been initialized by
        // some other mechanism.
        dataSource.setUrl("jdbc:hsqldb:mem:rightHandedTest");
        dataSource.setUsername("sa");
        dataSource.setInitialSize(4);
        dataSource.setDefaultAutoCommit(false);

        LocalSessionFactoryBuilder sessionFactoryBuilder = new LocalSessionFactoryBuilder(dataSource);
        sessionFactoryBuilder.addResource("se/citerus/dddsample/infrastructure/persistence/hibernate/Location.hbm.xml") ;

        SessionFactory sessionFactory = sessionFactoryBuilder.buildSessionFactory();

        LocationRepositoryHibernate repo = new LocationRepositoryHibernate();
        repo.setSessionFactory(sessionFactory);

        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactory);
        TransactionTemplate tt = new TransactionTemplate(transactionManager);


        tt.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                        sessionFactory
                                .getCurrentSession()
                                .saveOrUpdate(whereShouldWeBe);
                    }
                }
        );


        List<Location> locations = tt.execute(
                new TransactionCallback<List<Location>>() {
                    @Override
                    public List<Location> doInTransaction(TransactionStatus transactionStatus) {
                        return repo.findAll();
                    }
                }
        );

        Assert.assertEquals(1, locations.size());

        Location whereAreWe = tt.execute(
                new TransactionCallback<Location>() {
                    @Override
                    public Location doInTransaction(TransactionStatus transactionStatus) {
                        return repo.find(whereShouldWeBe.unLocode());
                    }
                }
        );

        Assert.assertEquals(whereShouldWeBe.unLocode().idString(), whereAreWe.unLocode().idString());
        Assert.assertEquals(whereShouldWeBe.name(), whereAreWe.name());
    }

}
