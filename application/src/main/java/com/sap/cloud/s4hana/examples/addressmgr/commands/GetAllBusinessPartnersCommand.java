package com.sap.cloud.s4hana.examples.addressmgr.commands;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sap.cloud.s4hana.examples.addressmgr.vdm.namespaces.businesspartner.BusinessPartner;
import com.sap.cloud.s4hana.examples.addressmgr.vdm.services.BusinessPartnerService;
import com.sap.cloud.sdk.cloudplatform.cache.CacheKey;
import com.sap.cloud.sdk.cloudplatform.logging.CloudLoggerFactory;
import com.sap.cloud.sdk.frameworks.hystrix.HystrixUtil;
import com.sap.cloud.sdk.s4hana.connectivity.CachingErpCommand;
import com.sap.cloud.sdk.s4hana.datamodel.odata.helper.Order;

public class GetAllBusinessPartnersCommand extends CachingErpCommand<List<BusinessPartner>>
{
    private static final Logger logger = CloudLoggerFactory.getLogger(GetAllBusinessPartnersCommand.class);

    private static final String CATEGORY_PERSON = "1";

    private static final Cache<CacheKey, List<BusinessPartner>> cache =
        CacheBuilder.newBuilder().maximumSize(50).expireAfterWrite(5, TimeUnit.MINUTES).build();

    private final BusinessPartnerService service;

    public GetAllBusinessPartnersCommand( final BusinessPartnerService service )
    {
        super(
            HystrixUtil.getDefaultErpCommandSetter(
                GetAllBusinessPartnersCommand.class,
                HystrixUtil.getDefaultErpCommandProperties().withExecutionTimeoutInMilliseconds(10000)));

        this.service = service;
    }

    @Nonnull
    @Override
    protected Cache<CacheKey, List<BusinessPartner>> getCache()
    {
        return cache;
    }

    @Nonnull
    @Override
    protected List<BusinessPartner> runCacheable()
        throws Exception
    {
        final List<BusinessPartner> businessPartners =
            service
                .getAllBusinessPartner()
                .select(
                    BusinessPartner.BUSINESS_PARTNER,
                    BusinessPartner.LAST_NAME,
                    BusinessPartner.FIRST_NAME,
                    BusinessPartner.ADDR_LAST_CHECKED_BY_BUS,
                    BusinessPartner.ADDR_LAST_CHECKED_ON_BUS)
                .filter(BusinessPartner.BUSINESS_PARTNER_CATEGORY.eq(CATEGORY_PERSON))
                .orderBy(BusinessPartner.LAST_NAME, Order.ASC)
                .execute();

        return businessPartners;
    }

    @Override
    protected List<BusinessPartner> getFallback()
    {
        logger.warn("Fallback called because of exception:", getExecutionException());
        return Collections.emptyList();
    }
}
