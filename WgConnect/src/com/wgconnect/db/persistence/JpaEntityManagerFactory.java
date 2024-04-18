/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
 *
 * WgConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WgConnect.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.db.persistence;

import com.wgconnect.core.util.WgConnectLogger;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;

/**
 * JpaEntityManagerFactory
 *
 * The jpa entity manager factory.
 * 
 * @author: wgconnect@proton.me
 */
public class JpaEntityManagerFactory {

    private static final WgConnectLogger log = WgConnectLogger.getLogger(JpaEntityManagerFactory.class);

    private static JpaEntityManagerFactory INSTANCE;
    
    private final List<Class> entityClasses;
    private final Properties properties;
    private final DataSource dataSource;

    private JpaEntityManagerFactory(List<Class> entityClasses, Properties properties, DataSource dataSource) {
        this.entityClasses = entityClasses;
        this.properties = properties;
        this.dataSource = dataSource;
    }

    public static EntityManager getEntityManager(List<Class> entityClasses, Properties properties, DataSource dataSource) {
        return getEntityManagerFactory(entityClasses, properties, dataSource).createEntityManager();
    }

    private static EntityManagerFactory getEntityManagerFactory(List<Class> entityClasses, Properties properties, DataSource dataSource) {
        if (INSTANCE == null) {
            INSTANCE = new JpaEntityManagerFactory(entityClasses, properties, dataSource);
        }
        
        return INSTANCE.getEntityManagerFactory();
    }
    
    private EntityManagerFactory getEntityManagerFactory() {
        PersistenceUnitInfo persistenceUnitInfo =
            new WgConnectPersistenceUnitInfo(getClass().getSimpleName(), getEntityClassNames(), properties);
        Map<String, Object> configuration = new HashMap<>();

        return new EntityManagerFactoryBuilderImpl(new PersistenceUnitInfoDescriptor(persistenceUnitInfo), configuration)
            .withDataSource(dataSource)
            .build();
    }

    protected List<String> getEntityClassNames() {
        return entityClasses
            .stream()
            .map(Class::getName)
            .toList();
    }
}
