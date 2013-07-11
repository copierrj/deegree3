package org.deegree.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.deegree.spring.AutowireTestContext.TestBean;
import org.deegree.spring.AutowireTestContext.TestResource;
import org.deegree.workspace.Resource;
import org.deegree.workspace.ResourceIdentifier;
import org.deegree.workspace.ResourceManager;
import org.deegree.workspace.ResourceManagerMetadata;
import org.deegree.workspace.ResourceMetadata;
import org.deegree.workspace.ResourceProvider;
import org.deegree.workspace.Workspace;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;

public class WorkspaceBeanFactoryTest {

    Workspace workspace;

    @Before
    @SuppressWarnings("unchecked")
    public void mockWorkspace() {
        ResourceProvider<TestResource> resourceProvider = mock( ResourceProvider.class );
        when( resourceProvider.getNamespace() ).thenReturn( "" );

        List<ResourceMetadata<TestResource>> resourceMetadataList = new ArrayList<ResourceMetadata<TestResource>>();
        for ( int i = 0; i < 10; i++ ) {
            ResourceIdentifier<TestResource> resourceIdentifier = mock( ResourceIdentifier.class );
            when( resourceIdentifier.getId() ).thenReturn( "test" + i );

            ResourceMetadata<TestResource> resourceMetadata = mock( ResourceMetadata.class );
            when( resourceMetadata.getIdentifier() ).thenReturn( resourceIdentifier );
            when( resourceMetadata.getProvider() ).thenReturn( resourceProvider );

            resourceMetadataList.add( resourceMetadata );
        }

        ResourceManagerMetadata<TestResource> metadata = mock( ResourceManagerMetadata.class );
        when( metadata.getWorkspacePath() ).thenReturn( "test" );

        ResourceManager<TestResource> resourceManager = mock( ResourceManager.class );
        when( resourceManager.getMetadata() ).thenReturn( metadata );
        when( resourceManager.getResourceMetadata() ).thenReturn( resourceMetadataList );

        List<ResourceManager<? extends Resource>> resourceManagers = new ArrayList<ResourceManager<? extends Resource>>();
        resourceManagers.add( resourceManager );

        workspace = mock( Workspace.class );
        when( workspace.getModuleClassLoader() ).thenReturn( getClass().getClassLoader() );
        when( workspace.getResourceManagers() ).thenReturn( resourceManagers );
    }

    @Test
    public void testGetResourceClass() {
        WorkspaceBeanFactory beanFactory = new WorkspaceBeanFactory( workspace );

        assertEquals( ApplicationContextHolder.class,
                      beanFactory.getResourceClass( ApplicationContextHolderManager.class ) );
    }

    @Test
    public void testAutowire() {
        WorkspaceBeanFactory beanFactory = new WorkspaceBeanFactory( workspace ) {

            @Override
            @SuppressWarnings("rawtypes")
            protected Class<? extends Resource> getResourceClass( Class<? extends ResourceManager> managerClass ) {
                return TestResource.class;
            }
        };

        final GenericApplicationContext context = new GenericApplicationContext( beanFactory );
        final AnnotatedBeanDefinitionReader defReader = new AnnotatedBeanDefinitionReader( context );
        defReader.register( AutowireTestContext.class );
        context.refresh();

        final TestBean testBean = context.getBean( TestBean.class );
        assertNotNull( testBean );
        assertNotNull( testBean.testResource );
    }
    
    interface TestInterface<T> {        
    
    }
    
    static abstract class TestClass<U, V> implements TestInterface<U>{
        
    }
    
    @Test
    public void testGetTypes() {
        WorkspaceBeanFactory beanFactory = new WorkspaceBeanFactory( workspace );
        
        TestInterface<String> testInterface = new TestClass<String, Integer>() {};
        assertEquals(String.class, beanFactory.getTypes(testInterface.getClass(), TestInterface.class)[0]);
    }
}
