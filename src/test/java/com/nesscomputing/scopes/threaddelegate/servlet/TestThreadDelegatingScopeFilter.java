package com.nesscomputing.scopes.threaddelegate.servlet;

import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;


import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceFilter;
import com.nesscomputing.scopes.threaddelegate.ScopedObject;
import com.nesscomputing.scopes.threaddelegate.ThreadDelegatedScope;
import com.nesscomputing.scopes.threaddelegate.ThreadDelegatedScopeModule;
import com.nesscomputing.scopes.threaddelegate.ScopedObject.TestObjectProvider;
import com.nesscomputing.scopes.threaddelegate.servlet.ThreadDelegatingScopeFilter;

public class TestThreadDelegatingScopeFilter
{
    @Inject
    private ThreadDelegatingScopeFilter filter = null;

    @Inject
    private Injector injector = null;

    @Before
    public void setUp()
    {
        ThreadDelegatedScope.SCOPE.changeScope(null);

        final Injector injector = Guice.createInjector(Stage.PRODUCTION,
                                                       new ThreadDelegatedScopeModule(),
                                                       new AbstractModule() {
            @Override
            public void configure() {
                bind(ScopedObject.class).toProvider(TestObjectProvider.class).in(ThreadDelegatedScope.SCOPE);
            }
        });

        injector.injectMembers(this);
        Assert.assertNotNull(filter);
        Assert.assertNotNull(injector);
    }

    @After
    public void tearDown()
    {
        Assert.assertNotNull(filter);
        filter = null;

        Assert.assertNotNull(injector);
        // Get rid of the stupid "duplicate Servlet module warning"
        final GuiceFilter filter = injector.getInstance(GuiceFilter.class);
        filter.destroy();
        injector = null;

        ThreadDelegatedScope.SCOPE.changeScope(null);
    }


    @Test
    public void testSimpleFilter() throws Exception
    {
        final ScopedObject t1 = injector.getInstance(ScopedObject.class);
        Assert.assertNotNull(t1);

        final AtomicReference<ScopedObject> refHolder = new AtomicReference<ScopedObject>();

        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.replay(request);

        filter.doFilter(request, null, new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                final ScopedObject t2 = injector.getInstance(ScopedObject.class);
                Assert.assertNotNull(t2);
                refHolder.set(t2);
            }
        });

        final ScopedObject t2 = refHolder.get();
        Assert.assertNotNull(t2);
        final ScopedObject t3 = injector.getInstance(ScopedObject.class);
        Assert.assertNotNull(t3);

        Assert.assertNotSame(t1, t2);
        Assert.assertNotSame(t1, t3);
        Assert.assertNotSame(t2, t3);

        EasyMock.verify(request);
    }
}
