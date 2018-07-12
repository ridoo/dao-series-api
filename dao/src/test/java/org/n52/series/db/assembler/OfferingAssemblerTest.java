
package org.n52.series.db.assembler;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.n52.io.request.Parameters.HREF_BASE;
import static org.n52.io.request.Parameters.MATCH_DOMAIN_IDS;
import static org.n52.series.test.TestUtils.getIdAsString;

import java.util.List;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.n52.io.request.Parameters;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.TestBase;
import org.n52.series.db.TestRepositories;
import org.n52.series.db.TestRepositoryConfig;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@DataJpaTest
@ExtendWith(SpringExtension.class)
public class OfferingAssemblerTest extends TestBase {

    @Autowired
    private OfferingRepository offeringRepository;

    @Autowired
    private DatasetRepository<DatasetEntity> datasetRepository;

    @Autowired
    private TestRepositories testRepositories;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    private OfferingAssembler assembler;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        this.assembler = new OfferingAssembler(offeringRepository, datasetRepository);
        // Manually start autowiring on non-spring managed Object
        beanFactory.autowireBean(assembler);
    }

    @Test
    @DisplayName("Offering of non-public dataset is not found")
    public void given_aNonPublicDataset_when_queryingOfferings_then_offeringIsNotPartOfCollection() {
        final DatasetEntity dataset = quantityDataset("phen", "off", "proc", "sml", "feat", "featFormat");
        dataset.setPublished(false);

        Assertions.assertAll("Offering is not part of collection", () -> {
            assertThat(assembler.getAllCondensed(defaultQuery)).isEmpty();
        });

        Assertions.assertAll("Offering id does not exist", () -> {
            final String id = getIdAsString(dataset.getOffering());
            assertThat(assembler.exists(id, defaultQuery)).isFalse();
            assertThat(assembler.getInstance(id, defaultQuery)).isNull();
        });

        Assertions.assertAll("Offering identifier does not exist", () -> {
            final DbQuery matchDomainIds = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString());
            assertThat(assembler.exists("off", matchDomainIds)).isFalse();
            assertThat(assembler.getInstance("off", matchDomainIds)).isNull();
        });
    }

    @Test
    @DisplayName("Filtering works properly")
    public void given_publicDatasets_when_filteringViaParameters_then_outputContainsMatchingOfferings() {
        quantityDataset("ph1", "of1", "pr1", "format1", "fe1", "format2");
        quantityDataset("ph1", "of2", "pr2", "format3", "fe2", "format4");
        quantityDataset("ph2", "of3", "pr2", "format3", "fe2", "format4");

        Assertions.assertAll("Offerings with matching (by Domain ID) Phenomena filters", () -> {
            final DbQuery ph1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
                                           .replaceWith(Parameters.PHENOMENA, "ph1");
            final List<OfferingOutput> offerings = assembler.getAllCondensed(ph1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .anyMatch(it -> it.equals("of1"))
                                 .anyMatch(it -> it.equals("of2"))
                                 .noneMatch(it -> it.equals("of3"));
        });

        Assertions.assertAll("Offerings with matching (by Domain ID) Procedure filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.PROCEDURES, "pr1");

            List<OfferingOutput> offerings = assembler.getAllCondensed(pr1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .anyMatch(it -> it.equals("of1"))
                                 .noneMatch(it -> it.equals("of2"))
                                 .noneMatch(it -> it.equals("of3"));
        });


        Assertions.assertAll("Offerings with matching (by Domain ID) Feature filters", () -> {
            DbQuery fe2Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.FEATURES, "fe2");

            List<OfferingOutput> offerings = assembler.getAllCondensed(fe2Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .noneMatch(it -> it.equals("of1"))
                                 .anyMatch(it -> it.equals("of2"))
                                 .anyMatch(it -> it.equals("of3"));
        });

        Assertions.assertAll("Offerings with matching (by Domain ID) Offerings filters", () -> {
            DbQuery pr1Query = defaultQuery.replaceWith(MATCH_DOMAIN_IDS, TRUE.toString())
            							   .replaceWith(Parameters.OFFERINGS, "of1,of2");

            List<OfferingOutput> offerings = assembler.getAllCondensed(pr1Query);
            assertThat(offerings).extracting(OfferingOutput::getDomainId)
                                 .anyMatch(it -> it.equals("of1"))
                                 .anyMatch(it -> it.equals("of2"))
                                 .noneMatch(it -> it.equals("of3"));
        });

        //TODO: Test Services Filter
        //TODO: Test Categories Filter
        //TODO: Test Platforms Filter
        //TODO: Test Stations Filter
        //TODO: Test platformTypes Filter
        //TODO: Test valueTypes Filter
    }

    @Test
    @DisplayName("Offering output assembled properly")
    public void given_validDataset_when_queryingOffering_then_outputGetsAssembledProperly() {

    	final String offeringIdentifier = "off";
        final String offeringLabel = "TestLabel";

        final OfferingEntity offering = new OfferingEntity();
        offering.setIdentifier(offeringIdentifier);
        offering.setName(offeringLabel);
        testRepositories.save(offering);
        final DatasetEntity dataset = textDataset("phen", offeringIdentifier, "proc", "sml", "feat", "featFormat");

        final String expectedId = Long.toString(offering.getId());

        final DbQuery query = defaultQuery.replaceWith(HREF_BASE, "https://foo.com/");
        Assertions.assertAll("Assert members of serialized output assemble", () -> {
            final List<OfferingOutput> offerings = assembler.getAllCondensed(query);
            assertThat(offerings).element(0)
                                 .returns(expectedId, OfferingOutput::getId)
                                 .returns("off", OfferingOutput::getDomainId)
                                 .returns("https://foo.com/offerings/" + expectedId, OfferingOutput::getHref);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Condensed)", () -> {
            List<OfferingOutput> offerings = assembler.getAllCondensed(defaultQuery);

            ObjectAssert<OfferingOutput> element = assertThat(offerings).element(0);
            element.extracting(OfferingOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(OfferingOutput::getDomainId).allMatch(it -> it.equals(offeringIdentifier));
            element.extracting(OfferingOutput::getLabel).allMatch(it -> it.equals(offeringLabel));

            // Does not return unserialized fields
            element.extracting(OfferingOutput::getExtras).allMatch(it -> it == null);
            element.extracting(OfferingOutput::getService).allMatch(it -> it == null);
            element.extracting(OfferingOutput::getHref).allMatch(it -> it == null);
        });

        Assertions.assertAll("Assert members of serialized output assemble (Expanded)", () -> {
            List<OfferingOutput> offerings = assembler.getAllExpanded(defaultQuery);

            ObjectAssert<OfferingOutput> element = assertThat(offerings).element(0);
            element.extracting(OfferingOutput::getId).allMatch(it -> it.equals(expectedId));
            element.extracting(OfferingOutput::getDomainId).allMatch(it -> it.equals(offeringIdentifier));
            element.extracting(OfferingOutput::getLabel).allMatch(it -> it.equals(offeringLabel));
            //TODO: Check if getExtras is supposed to return null or empty collection
            element.extracting(OfferingOutput::getExtras).allMatch(it -> it == null);

            element.extracting(OfferingOutput::getService).allMatch(it ->
            			((ServiceOutput)it).getLabel().equals("TestService") &&
						((ServiceOutput)it).getId().equals("42")
			);
        });
    }

    @SpringBootConfiguration
    @EnableJpaRepositories(basePackageClasses = DatasetRepository.class)
    static class Config extends TestRepositoryConfig<DatasetEntity> {
        public Config() {
            super("/mapping/core/persistence.xml");
        }

        @Override
        public TestRepositories testRepositories() {
            return new TestRepositories();
        }

        @Bean
        public ServiceEntity serviceEntity() {
            ServiceEntity serviceEntity = new ServiceEntity();
            serviceEntity.setId(42L);
            serviceEntity.setVersion("2.0");
            serviceEntity.setName("TestService");
            serviceEntity.setNoDataValues("-9999");
            return serviceEntity;
        }
    }
}