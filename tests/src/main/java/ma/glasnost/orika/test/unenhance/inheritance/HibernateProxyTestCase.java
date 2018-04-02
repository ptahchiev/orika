/*
 * Orika - simpler, better and faster Java bean mapping
 *
 * Copyright (C) 2011-2013 Orika authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ma.glasnost.orika.test.unenhance.inheritance;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.unenhance.HibernateUnenhanceStrategy;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:HibernateProxyTestCase-context.xml")
@Transactional
@DirtiesContext
public class HibernateProxyTestCase {
	@Autowired
	private SessionFactory sessionFactory;

	private Serializable sub1Id;
	private Serializable sub21Id;
	private Serializable sub22Id;

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	@Before
	public void setup() {
		Sub1Entity sub1 = new Sub1Entity();
		sub1.setMyProperty("my property on sub1");
		sub1.setSub1Property(1);
		sub1Id = getSession().save(sub1);

		Sub2Entity sub21 = new Sub2Entity();
		sub21.setMyProperty("my property on sub2");
		sub21.setSub2Property(2);
		sub21.setReferences(sub1);
		sub21Id = getSession().save(sub21);

		Sub2Entity sub22 = new Sub2Entity();
		sub22.setMyProperty("my property on sub2-2");
		sub22.setSub2Property(3);
		sub22.setReferences(sub21);
		sub22Id = getSession().save(sub22);

		getSession().flush();
		getSession().clear();
	}

	private MapperFacade buildMapper() {
		MapperFactory mf = new DefaultMapperFactory.Builder()
				.unenhanceStrategy(new HibernateUnenhanceStrategy()).build();
		mf.registerClassMap(mf
				.classMap(AbstractEntity.class, AbstractDTO.class).byDefault()
				.toClassMap());
		mf.registerClassMap(mf.classMap(Sub1Entity.class, Sub1EntityDTO.class)
				.use(AbstractEntity.class, AbstractDTO.class).byDefault()
				.toClassMap());
		mf.registerClassMap(mf.classMap(Sub2Entity.class, Sub2EntityDTO.class)
				.use(AbstractEntity.class, AbstractDTO.class).byDefault()
				.toClassMap());
		return mf.getMapperFacade();
	}

	@Test
	public void testMappingNonProxyObject() {
		MapperFacade mapper = buildMapper();

		Sub2Entity sub2 = (Sub2Entity) getSession().get(Sub2Entity.class,
				sub21Id);
		sub2.setReferences((MyEntity) ((HibernateProxy) sub2.getReferences())
				.getHibernateLazyInitializer().getImplementation());
		Sub2EntityDTO sub2Dto = mapper.map(sub2, Sub2EntityDTO.class);

		Assert.assertEquals(sub2.getMyProperty(), sub2Dto.getMyProperty());
		Assert.assertEquals(sub2.getSub2Property(), sub2Dto.getSub2Property());
		Assert.assertNotNull(sub2Dto.getReferences());
		Assert.assertEquals(sub2.getReferences().getMyProperty(), sub2Dto
				.getReferences().getMyProperty());
		Assert.assertEquals(Sub1EntityDTO.class, sub2Dto.getReferences()
				.getClass());
		Assert.assertEquals(1,
				((Sub1EntityDTO) sub2Dto.getReferences()).getSub1Property());
	}

	@Test
	public void testMappingProxyObject() {
		MapperFacade mapper = buildMapper();

		Sub2Entity sub2 = (Sub2Entity) getSession().get(Sub2Entity.class,
				sub21Id);
		Sub2EntityDTO sub2Dto = mapper.map(sub2, Sub2EntityDTO.class);

		Assert.assertEquals(sub2.getMyProperty(), sub2Dto.getMyProperty());
		Assert.assertEquals(sub2.getSub2Property(), sub2Dto.getSub2Property());
		Assert.assertNotNull(sub2Dto.getReferences());
		Assert.assertEquals(sub2.getReferences().getMyProperty(), sub2Dto
				.getReferences().getMyProperty());
		Assert.assertEquals(Sub1EntityDTO.class, sub2Dto.getReferences()
				.getClass());
		Assert.assertEquals(1,
				((Sub1EntityDTO) sub2Dto.getReferences()).getSub1Property());
	}

	@Test
	public void testMappingCaching() {
		MapperFacade mapper = buildMapper();

		Sub2Entity sub22 = (Sub2Entity) getSession().get(Sub2Entity.class,
				sub22Id);
		Sub2Entity sub21 = (Sub2Entity) getSession().get(Sub2Entity.class,
				sub21Id);
		Sub2EntityDTO sub21Dto = mapper.map(sub21, Sub2EntityDTO.class);
		Sub2EntityDTO sub22Dto = mapper.map(sub22, Sub2EntityDTO.class);
		
		Assert.assertEquals(Sub1EntityDTO.class, sub21Dto.getReferences().getClass());
		Assert.assertEquals(Sub2EntityDTO.class, sub22Dto.getReferences().getClass());
	}

	@Test
	public void testMappingCollections() {
		MapperFacade mapper = buildMapper();

		List<MyEntity> entities = Arrays.asList((MyEntity) getSession().load(MyEntity.class, sub22Id),
				(MyEntity) getSession().load(MyEntity.class, sub21Id),
				(MyEntity) getSession().load(MyEntity.class, sub1Id));
		
		List<MyEntityDTO> mappedList = mapper.mapAsList(entities, MyEntityDTO.class);
		Assert.assertEquals(Sub2EntityDTO.class, mappedList.get(0).getClass());
		Assert.assertEquals(Sub2EntityDTO.class, mappedList.get(1).getClass());
		Assert.assertEquals(Sub1EntityDTO.class, mappedList.get(2).getClass());
	}
}
