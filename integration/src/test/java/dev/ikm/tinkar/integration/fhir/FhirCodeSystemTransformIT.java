/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.integration.fhir;

import dev.ikm.tinkar.common.id.IntIds;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.coordinate.stamp.*;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculatorWithCache;
import dev.ikm.tinkar.entity.*;
import dev.ikm.tinkar.fhir.transformers.FhirCodeSystemTransform;
import dev.ikm.tinkar.fhir.transformers.FhirStatedDefinitionTransformer;
import dev.ikm.tinkar.integration.TestConstants;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.hl7.fhir.r4.model.CodeSystem;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FhirCodeSystemTransformIT {
    FhirCodeSystemTransform fhirCodeSystemTransform;
    FhirStatedDefinitionTransformer fhirStatedDefinitionTransformer;
    StampCalculator stampCalculator;
    private static final Logger LOG = LoggerFactory.getLogger(FhirCodeSystemTransformIT.class);
    final int transformSize = 1000;
    File dataStore = new File(System.getProperty("user.home") + "/Solor/SnomedCT_US_20230901_SpinedArray-20240402");

    //@BeforeAll
    public void setup() {
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, dataStore);
       // FileUtil.recursiveDelete(dataStore);
        PrimitiveData.selectControllerByName(TestConstants.SA_STORE_OPEN_NAME);
        PrimitiveData.start();
    }

    //@AfterAll
    public void teardown() {
        PrimitiveData.stop();
    }

    //@Test
    public void testFhirCodeSystemTransformation()  throws IOException {

//        File file = TestConstants.TINK_TEST_FILE;
//        LoadEntitiesFromDtoFile loadDTO = new LoadEntitiesFromDtoFile(file);
//        int count = loadDTO.compute();
//        LOG.info(count + " entitles loaded from file: " + loadDTO.report() + "\n\n");


        int patternNid = TinkarTerm.DESCRIPTION_PATTERN.nid();
        Set<Integer> pathNids = new HashSet<>();
        EntityService.get().forEachSemanticOfPattern(patternNid,patternEntity1 ->
                patternEntity1.stampNids().forEach(stampNid -> {
                    StampEntity<? extends StampEntityVersion> stamp = EntityService.get().getStampFast(stampNid);
                    pathNids.add(stamp.pathNid());
                })
        );

        AtomicInteger counter = new AtomicInteger();
        AtomicInteger totalConcepts = new AtomicInteger();
        pathNids.forEach(pathNid -> {
            if(TinkarTerm.DEVELOPMENT_PATH.nid() == pathNid){

                StampCalculator stampCalculatorWithCache =   initStampCalculator(pathNid);
                List<ConceptEntity<? extends  ConceptEntityVersion>> concepts = new ArrayList<>();
                PrimitiveData.get().forEachConceptNid((conceptnid) -> {
                    if(counter.get() == transformSize){
                       totalConcepts.set(totalConcepts.get()+transformSize);
                       processConceptBatch(concepts, stampCalculatorWithCache, totalConcepts.get());
                       concepts.clear();
                       counter.set(0);
                    }
                    if(counter.get() < transformSize) {
                        Entity<EntityVersion> entity = EntityService.get().getEntityFast(conceptnid);
                        if (entity instanceof ConceptEntity conceptEntity) {
                            concepts.add(conceptEntity);
                            counter.getAndIncrement();
                        }
                    }
                });
                if(counter.get() < transformSize && counter.get() > 0){
                    totalConcepts.set(totalConcepts.get()+counter.get());
                    processConceptBatch(concepts, stampCalculatorWithCache, totalConcepts.get());
                }
            }
        });

    }

    private void processConceptBatch(List<ConceptEntity<? extends ConceptEntityVersion>> concepts, StampCalculator stampCalculatorWithCache, int conceptCount) {
        LOG.debug("Processing Concepts : " + concepts.size() + "   TOTAL CONCEPTS : " + conceptCount);
        fhirCodeSystemTransform= new FhirCodeSystemTransform(stampCalculatorWithCache, concepts, (fhirProvenanceString) -> {
            Assertions.assertNotNull(fhirProvenanceString);
            Assertions.assertFalse(fhirProvenanceString.isEmpty());
        });
        try {
            fhirCodeSystemTransform.compute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @Test
//    @DisplayName("Test transformation of axiom semantics.")
    public void testTransformAxiomSemantics() {

        String toTime = "2019-10-22T12:31:04";
        LocalDateTime toLocalDateTime = LocalDateTime.parse(toTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long toTimeStamp = toLocalDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        int elPlusPlusPatternNId = TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid();

        AtomicInteger counter = new AtomicInteger(0);
        if(counter.getAndIncrement() < 1){
            EntityService.get().forEachSemanticOfPattern(elPlusPlusPatternNId, semanticEntity -> {
                stampCalculator=initStampCalculator(TinkarTerm.DEVELOPMENT_PATH.nid(), toTimeStamp);

                fhirStatedDefinitionTransformer = new FhirStatedDefinitionTransformer(initStampCalculator());
                List<CodeSystem.ConceptPropertyComponent> statedAxiomPropertiesResult =
                        fhirStatedDefinitionTransformer.transfromAxiomSemantics(semanticEntity, TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN);
                Assertions.assertNotNull(statedAxiomPropertiesResult);
                Assertions.assertFalse(statedAxiomPropertiesResult.isEmpty());
            });
        }
    }

    private StampCalculator initStampCalculator(Integer pathNid) {
        return initStampCalculator(pathNid, Long.MAX_VALUE);
    }

    private StampCalculatorWithCache initStampCalculator(Integer pathNid, long toTimeStamp) {
        StampPositionRecord stampPositionRecord = StampPositionRecordBuilder.builder().time(toTimeStamp).pathForPositionNid(pathNid).build();
        StampCoordinateRecord stampCoordinateRecord = StampCoordinateRecordBuilder.builder()
                .allowedStates(StateSet.ACTIVE_AND_INACTIVE)
                .stampPosition(stampPositionRecord)
                .moduleNids(IntIds.set.empty())
                .build().withStampPositionTime(toTimeStamp);
        return stampCoordinateRecord.stampCalculator();
    }

    private StampCalculatorWithCache initStampCalculator() {
        return initStampCalculator(TinkarTerm.DEVELOPMENT_PATH.nid(), Long.MAX_VALUE);
    }

}
