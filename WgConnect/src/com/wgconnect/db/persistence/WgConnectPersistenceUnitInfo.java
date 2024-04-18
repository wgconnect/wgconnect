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

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * WgConnectPersistenceUnitInfo
 * 
 * The main class for the WgConnect persistence unit info.
 * 
 * @author: wgconnect@proton.me
 */
public class WgConnectPersistenceUnitInfo implements PersistenceUnitInfo {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(WgConnectPersistenceUnitInfo.class);

    public static String JPA_VERSION = "2.1";
    private final String persistenceUnitName;
    private final PersistenceUnitTransactionType transactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL;
    private final List<String> managedClassNames;
    private final List<String> mappingFileNames = new ArrayList<>();
    private final Properties properties;
    private DataSource jtaDataSource;
    private DataSource nonjtaDataSource;
    private final List<ClassTransformer> transformers = new ArrayList<>();
    
    private final List<URL> jarFileUrls = new ArrayList<>();
    private final URL persistenceUnitRootUrl = null;
    private final boolean excludeUnlistedClassesBool = true;
    private final SharedCacheMode sharedCacheMode = null;
    private final ValidationMode validationMode = null;
    private final String persistenceXMLSchemaVersion = null;
    
    public WgConnectPersistenceUnitInfo(String persistenceUnitName, List<String> managedClassNames, Properties properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.managedClassNames = managedClassNames;
        this.properties = properties;
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return this.getClass().getName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        return jtaDataSource;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return nonjtaDataSource;
    }

    @Override
    public List<String> getMappingFileNames() {
        return mappingFileNames;
    }

    @Override
    public List<URL> getJarFileUrls() {
        return jarFileUrls;
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitRootUrl;
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return excludeUnlistedClassesBool;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    @Override
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return persistenceXMLSchemaVersion;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return null;
    }
}
