package org.n52.series.springdata;


import org.n52.series.db.beans.PhenomenonEntity;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface PhenomenonRepository extends ParameterDataRepository<PhenomenonEntity> {

}