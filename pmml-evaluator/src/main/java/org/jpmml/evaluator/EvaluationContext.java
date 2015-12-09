/*
 * Copyright (c) 2013 Villu Ruusmann
 *
 * This file is part of JPMML-Evaluator
 *
 * JPMML-Evaluator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-Evaluator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-Evaluator.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.evaluator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.dmg.pmml.DefineFunction;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMMLObject;

abstract
public class EvaluationContext {

	private Map<FieldName, FieldValue> fields = new HashMap<>();

	private List<String> warnings = new ArrayList<>();


	abstract
	public FieldValue createFieldValue(FieldName name, Object value);

	abstract
	public Result<DerivedField> resolveDerivedField(FieldName name);

	abstract
	public Result<DefineFunction> resolveFunction(String name);

	public FieldValue evaluate(FieldName name){
		Map.Entry<FieldName, FieldValue> entry = getFieldEntry(name);
		if(entry != null){
			return entry.getValue();
		}

		EvaluationContext.Result<DerivedField> result = resolveDerivedField(name);
		if(result != null){
			FieldValue value = ExpressionUtil.evaluate(result.getElement(), this);

			return declare(name, value);
		}

		throw new MissingFieldException(name);
	}

	public FieldValue getField(FieldName name){
		Map<FieldName, FieldValue> fields = getFields();

		return fields.get(name);
	}

	public Map.Entry<FieldName, FieldValue> getFieldEntry(FieldName name){
		Map<FieldName, FieldValue> fields = getFields();

		if(fields.containsKey(name)){
			FieldValue value = fields.get(name);

			return new AbstractMap.SimpleImmutableEntry<>(name, value);
		}

		return null;
	}

	public FieldValue declare(FieldName name, Object value){

		if(value instanceof FieldValue){
			return declare(name, (FieldValue)value);
		}

		return declare(name, createFieldValue(name, value));
	}

	public FieldValue declare(FieldName name, FieldValue value){
		Map<FieldName, FieldValue> fields = getFields();

		boolean declared = fields.containsKey(name);
		if(declared){
			throw new DuplicateValueException(name);
		}

		fields.put(name, value);

		return value;
	}

	void declareAll(Map<FieldName, ?> values){
		Collection<? extends Map.Entry<FieldName, ?>> entries = values.entrySet();

		for(Map.Entry<FieldName, ?> entry : entries){
			declare(entry.getKey(), entry.getValue());
		}
	}

	public void addWarning(String warning){
		List<String> warnings = getWarnings();

		warnings.add(warning);
	}

	public Map<FieldName, FieldValue> getFields(){
		return this.fields;
	}

	public List<String> getWarnings(){
		return this.warnings;
	}

	<E extends PMMLObject> Result<E> createResult(E element){

		if(element != null){
			return new Result<>(element);
		}

		return null;
	}

	public class Result<E extends PMMLObject> {

		private E element = null;


		Result(E element){
			setElement(Objects.requireNonNull(element));
		}

		public EvaluationContext getContext(){
			return EvaluationContext.this;
		}

		public E getElement(){
			return this.element;
		}

		private void setElement(E element){
			this.element = element;
		}
	}
}