package odml.core;

/************************************************************************
 * odML - open metadata Markup Language - Copyright (C) 2009, 2010 Jan Grewe, Jan Benda
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License (LGPL) as published by the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * odML is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this software. If not, see
 * <http://www.gnu.org/licenses/>.
 */
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.swing.tree.TreeNode;
import org.slf4j.*;

/**
 * The {@link Property} class constitutes the building block of any odML file that actually stores the information. A {@link Property} 
 * must have a name and at least one {@link Value}. 
 *
 * <ol>
 * <li>name - mandatory, the name of the property.</li>
 * <li>value - mandatory, its value.</li>
 * <li>definition - optional, a descriptive text that defines the meaning of this property.</li>
 * <li>dependency - optional, mainly used in the terminologies to help GUIs and editors to assist the user entering
 * values. The dependency defines if this property should occur only under certain conditions. I.e. this property
 * occurs, makes sense, only if there also is a certain other property.</li>
 * <li>dependencyValue - optional, as the dependency entry: defines that this property is commonly depends on a certain
 * parent property that contains a specific parent value.</li>
 * <li>mapping - optional</li>
 * </ol>
 *
 * @since 08.2009
 * @author Jan Grewe, Christine Seitz
 *
 */
public class Property extends Object implements Serializable, Cloneable, TreeNode {

   protected static final Logger logger           = LoggerFactory.getLogger(Property.class);
   private static final long     serialVersionUID = 147L;
   private String                name             = "", dependency = "",
         dependencyValue = "", definition = "";
   private URL                   mappingURL;
   private Section               parentSection    = null;
   private Vector<Value>         values;
   public static Object[]        columns          = { "name", "reference", "value", "uncertainty",
                                                  "unit",
                                                  "type", "filename",
                                                  "valueDefinition", "propertyDefinition",
                                                  "dependency", "dependencyValue" };
   public static int             MATCH_ERROR      = -1, MATCH_NO = 0,
         MATCH_FIRST_CONFLICT_LAST_MATCH = 5, MATCH_INITIALS_ONLY = 10,
         MATCH_FIRST_OR_LAST_ONLY = 20, MATCH_FIRST_INITIAL_LAST = 30, MATCH_FIRST_LAST = 50,
         MATCH_EXACT = 50;


   public Property(String name) throws Exception {
      this(name, null, null, null, null);
   }


   /**
    * Default constructor for creating a property. Only name and the value must be given.
    *
    * @param name
    * {@link String}: the name of the new property, mandatory
    * @param value
    * {@link Object}: the value the property shall contain, mandatory except for terminologies
    * @throws Exception
    */
   public Property(String name, Object value) throws Exception {
      this(name, value, null, null, null);
   }


   /**
    *
    * @param name
    * {@link String}: the name of the new property, mandatory
    * @param value
    * {@link Object}: the value the property shall contain, mandatory except for terminologies
    * @param type
    * {@link String}
    * @throws Exception
    */
   public Property(String name, Object value, String type) throws Exception {
      this(name, value, null, null, type);
   }


   /**
    *
    * @param name
    * {@link String}: the name of the new property, mandatory
    * @param value
    * {@link Object}: the value the property shall contain, mandatory except for terminologies
    * @param unit
    * {@link String}
    * @param uncertainty
    * {@link Object}
    * @param type
    * {@link String}
    * @throws Exception
    */
   public Property(String name, Object value, String unit, Object uncertainty, String type)
                                                                                           throws Exception {
      this(name, null, value, unit, uncertainty, type, null, null, null, null, null, null);
   }


   /**
    * Creates a Property from a Vector containing the property data in the following sequence:
    * "name","reference","value","unit","uncertainty","type","fileName","valueDefinition","propertyDefinition",
    * "dependency","dependencyValue", "synonym","mappingURL"
    *
    * @param data
    * {@link Vector} of Objects that contains the data in the sequence as the {@link Property}.columns
    * @throws Exception
    */
   public Property(Vector<Object> data) throws Exception {
      this((String) data.get(0), (String) data.get(1), data.get(2), (String) data.get(3), data
            .get(4), (String) data
            .get(5), (String) data.get(6), (String) data.get(8), (String) data.get(7),
            (String) data.get(9),
            (String) data.get(10), (URL) data.get(11));
   }


   /**
    * Creates a Property directly from a Value-Vector; used by the Reader
    *
    * @param name
    * {@link String}: the name of the new property, mandatory
    * @param values
    * {@link Vector}<Value>: the values the property shall contain, mandatory except for terminologies
    * @param definition
    * {@link String}
    * @param dependency
    * {@link String}
    * @param dependencyValue
    * {@link String}
    * @param mapping
    * {@link URL}
    * @throws Exception
    */
   public Property(String name, Vector<Value> values, String definition, String dependency,
                   String dependencyValue,
                   URL mapping) throws Exception {
      try {
         initialize(name, values, definition, dependency, dependencyValue, mapping);
      } catch (Exception l) {
         logger.error("Could not create property: ", l.getLocalizedMessage());
      }
   }


   /**
    * Constructor for a property with a single value and according further informations. Any of the arguments may be
    * null except for the name of the property.
    *
    * @param name
    * {@link String}: the name of the new property, mandatory
    * @param reference
    * {@link String}
    * @param value
    * {@link String}: the value the property shall contain, mandatory except for terminologies
    * @param unit
    * {@link String}
    * @param uncertainty
    * {@link Object}
    * @param type
    * {@link String}
    * @param filename
    * {@link String}
    * @param definition {@link String}
    * @param valueDefinition {@link String}
    * @param dependency {@link String}
    * @param dependencyValue {@link String}
    * @param mapping {@link URL}
    * @throws Exception
    */
   public Property(String name, String reference, Object value, String unit, Object uncertainty,
                   String type,
                   String filename, String definition, String valueDefinition,
                   String dependency,
                   String dependencyValue, URL mapping) throws Exception {
      Vector<Value> theValues = new Vector<Value>();
      if (type == null || type.isEmpty()) {
         type = Value.inferOdmlType(value);
      }
      try {
         theValues.add(new Value(value, unit, uncertainty, type, filename,
               valueDefinition, reference));
         initialize(name, theValues, definition, dependency, dependencyValue, mapping);
      } catch (Exception l) {
         logger.error("Could not create property: ", l.getLocalizedMessage());
      }
   }


   /**
    * Constructor for a property with more than one value and the according other informations. Any of the arguments
    * may be null except for the name of the property.
    *
    * @param name {@link String} The name of the property.
    * @param values {@link Vector} that will be stored in the values.
    * @param references {@link Vector} of Strings, the references of the Values
    * @param unit {@link String}: The unit of the values. 
    * @param uncertainties {@link Vector}<Object> vector of value uncertainties.
    * @param type {@link String} value data type. There is only one type since mixing different types is not allowed.
    * @param filenames {@link Vector}<Object>:  vector if filenames
    * @param valueDefinitions {@link Vector}<String>: Vector of value definitions. 
    * @param definition {@link String} The property definition
    * @param dependency {@link String} Property dependence.
    * @param dependencyValue {@link String} the value the dependence Property should have.
    * @param mapping {@link URL} A url to a terminology to which this property should be mapped, if needed.
    * @throws Exception
    */
   public Property(String name, Vector<Object> values, Vector<String> references, String unit,
                   Vector<Object> uncertainties,
                   String type, Vector<String> filenames, Vector<String> valueDefinitions,
                   String definition,
                   String dependency, String dependencyValue, URL mapping) throws Exception {
      String message = "";
      if (name == null || name.isEmpty()) {
         message = "Could not create property! 'name' is mandatory entry and must not be null or empty!";
      } else if (name.contains("/")) {
         message = "Could not create property! 'name' must not be like a path (e.g. contain '/')!";
      } else if (values.size() < uncertainties.size() || values.size() < filenames.size()) {
         message = "Could not create property! There must not be more errors or definitions than there are values.";
      }
      if (!message.isEmpty()) {
         throw new Exception(message);
      }
      Object tmpUncertainty = null;
      String tmpFilenames = null;
      String tmpComment = null;
      String tmpReference = null;

      Vector<Value> theValues = new Vector<Value>();
      for (int i = 0; i < values.size(); i++) {
         if (uncertainties.size() > 0)
            tmpUncertainty = uncertainties.get(i);
         if (filenames.size() > 0)
            tmpFilenames = filenames.get(i);
         if (references.size() > 0)
            tmpReference = references.get(i);

         try {
            Value value = new Value(values.get(i), unit, tmpUncertainty, type,
                  tmpFilenames,
                  tmpComment, tmpReference);
            theValues.add(value);
         } catch (Exception e) {
            logger.error("Error during creation of value: ", e.getLocalizedMessage());
         }
      }
      initialize(name, theValues, definition, dependency, dependencyValue, mapping);
   }


   /**
    * Called by the Property-constructor to initialize the given components, i.e. checking for isEmpty or ==null (doing
    * nothing when so except for mandatory name, there throwing error)
    *
    * @param name {@link String} : the Property name.
    * @param values {@link Vector}<Object>: A Vector of values.
    * @param unit {@link String} : 
    * @param uncertainties {@link Vector}<Object>: Uncertainties related to the values.
    * @param type {@link String}:  
    * @param 4
    * Vector<String>
    * @param definition
    * @param valueComments
    * Vector<String>
    * @param dependency
    * String
    * @param dependencyValue
    * String
    * @param mapping
    * {@link URL}
    * @throws Exception
    */
   private void initialize(String name, Vector<Value> values, String definition, String dependency,
                           String dependencyValue, URL mapping) throws Exception {
      if (name.contains("/")) {
         throw new Exception("Property name must not be like a path!");
      }
      this.name = name;
      this.values = values;
      for (int i = 0; i < this.values.size(); i++) {
         this.values.get(i).setAssociatedProperty(this);
      }
      setDefinition(definition);
      setDependency(dependency);
      setDependencyValue(dependencyValue);
      setMapping(mappingURL);
   }


   /**
    * Sets the parent section of this property.
    *
    * @param section
    * {@link Section} the parent section. Can be null.
    */
   public void setParent(Section section) {
      this.parentSection = section;
   }


   /**
    * Returns the parent section of this property.
    *
    * @return {@link Section}: the parent section. returned value may be null.
    */
   public Section getParent() {
      return this.parentSection;
   }


   /**
    * Set the name of the property
    *
    * @param name
    * {@link String} the name of the property.
    */
   public void setName(String name) {
      this.name = name;
   }


   /**
    * Get the property name.
    *
    * @return {@link String}: the name of this property.
    */
   public String getName() {
      return this.name;
   }


   /**
    * Set the definition of this property, i.e. the definition that tells what this property means. Can also be used
    * for removing the definition by passing null.
    *
    * @param definition
    * {@link String} the definition. May be used to remove it.
    */
   public void setDefinition(String definition) {
      if (definition != null)
         this.definition = definition;
      else
         definition = "";
   }


   /**
    * Returns the definition stored for this Property.
    *
    * @return {@link String} the definition or an empty string.
    */
   public String getDefinition() {
      return this.definition;
   }


   /**
    * Get the name of the property this depends on.
    *
    * @return {@link String} the name of the dependency property.
    */
   public String getDependency() {
      if (dependency != null && dependency.isEmpty()) {
         this.dependency = "";
      }
      return this.dependency;
   }


   /**
    * Get the dependency property's value.
    *
    * @return - {@link String}: the value, the dependency property has to assume to make this property meaningful.
    */
   public String getDependencyValue() {
      if (dependencyValue != null && dependencyValue.isEmpty()) {
         this.dependencyValue = "";
      }
      return this.dependencyValue;
   }


   /**
    * Returns the content of the first value as {@link Float}. If content cannot be converted to float Float.NaN is
    * returned.
    *
    * @return {@link Float} the converted contend, or Float.NaN if conversion fails.
    */
   public double getNumber() {
      return getNumber(0);
   }


   /**
    * Returns the content of the i-th value as {@link Float}. If the content cannot be converted to float Float.NaN is
    * returned.
    *
    * @param i
    * {@link Integer} the value index.
    * @return {@link Float} the converted contend, or Float.NaN if conversion fails.
    */
   public double getNumber(int i) {
      DecimalFormat myDF = new DecimalFormat();
      myDF.setMaximumFractionDigits(15);
      myDF.setMinimumFractionDigits(2);
      myDF.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
      myDF.setGroupingUsed(false);
      try {
         return (myDF.parse(getValue(i).toString())).doubleValue();
      } catch (ParseException e) {
         logger.error("Value " + i + " can not be converted to float!");
         return Double.NaN;
      }
   }


   /**
    * Returns the first value's content as {@link String}.
    *
    * @return {@link String} the content as text.
    */
   public String getText() {
      return getText(0);
   }


   /**
    * Returns the i-th value's content as {@link String}.
    *
    * @param i
    * {@link Integer} the value index.
    * @return {@link String} the content as text.
    */
   public String getText(int i) {
      return getValue(i).toString();
   }


   /**
    * Returns the date component of first value's content if possible. Null otherwise.
    *
    * @return {@link Date} the date component if possible (yyyy-MM-dd format). Null, otherwise.
    */
   public Date getDate() {
      return getDate(0);
   }


   /**
    * Returns the date component of i-th value's content if possible. Null otherwise.
    *
    * @param i
    * {@link Integer} the value index.
    * @return {@link Date} the date component if possible (yyyy-MM-dd format). Null, otherwise.
    */
   public Date getDate(int i) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      try {
         return sdf.parse(getValue(i).toString());
      } catch (Exception e) {
         logger.error("Value could not be converted to a date entry.");
         return null;
      }
   }


   /**
    * Returns the time component of first value's content if possible. Null otherwise.
    *
    * @return {@link Date} the time component if possible (HH:mm:ss format). Null, otherwise.
    */
   public Date getTime() {
      return getTime(0);
   }


   /**
    * Returns the time component of i-th value's content if possible. Null otherwise.
    *
    * @param i
    * {@link Integer} the value index.
    * @return {@link Date} the time component if possible (HH:mm:ss format). Null, otherwise.
    */
   public Date getTime(int i) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      try {
         return sdf.parse(getValue(i).toString());
      } catch (Exception e) {
         logger.error("Value could not be converted to a time entry.");
         return null;
      }
   }


   public boolean addValue(Object value, String id, Object uncertainty, String filename,
                           String definition) {
      return addValue(value, id, null, uncertainty, null, filename, definition);
   }


   public boolean addValue(Object value) {
      return addValue(value, null, null, null, null);
   }


   public boolean addValue(Object value, String unit) {
      return addValue(value, null, unit, null, null, null, null);
   }


   public boolean addValue(Object value, String unit, Object uncertainty) {
      return addValue(value, unit, uncertainty, null, null);
   }


   public boolean addValue(Object value, String unit, Object uncertainty, String type) {
      return addValue(value, null, unit, uncertainty, type, null, null);
   }


   /**
    * Adds a new Value-class instance to this property. The function refuses adding if the value already exists in the
    * property. All arguments except the value can be null or empty. Method also checks whether type of value to add is
    * same, if not throwing warning as not known which type is correct as already existing ones
    *
    * @param value
    * {@link Object}: the new value = value-content.
    * @param id
    * {@link String}: the id of the value
    * @param unit
    * {@link String}
    * @param uncertainty
    * {@link Object}: the error estimation of this value.
    * @param type
    * {@link String}
    * @param filename
    * {@link String}: the default filename which is only of use when binary data is stored.
    * @param comment
    * {@link String}: the definition of this value. * @return {@link Boolean}: true if the operation
    * succeeded. False if the value is null, or the value already exists in the property.
    * @throws Exception
    */
   public boolean addValue(Object value, String id, String unit, Object uncertainty, String type,
                           String filename, String comment) {
      if (value == null) {
         logger.error("! the value to add must not be null or empty!");
         return false;
      }
      try {
         if (unit == null && this.valueCount() > 0) {
            unit = this.getUnit(0);
         }
         Value toAdd = new Value(value, unit, uncertainty, type, filename, comment, id);
         toAdd.setAssociatedProperty(this);
         if (values.contains(toAdd)) {
            logger.error("! value to add already existing in property!");
            return false;
         }
         values.add(toAdd);
         if (type != null && (!type.isEmpty())) {
            if ((values.get(0).getType() != null) && (!values.get(0).getType().isEmpty())
                  && (!type.equalsIgnoreCase(values.get(0).getType()))) {
               logger.warn("! type of newly added value (" + type
                     + ") differs from the one of the first value "
                     + "of the proprty (" + values.get(0).getType()
                     + ") > should be the same! index of newly "
                     + "added value is " + (values.size() - 1)
                     + "; can be updated later with setType(newType)");
            } else if ((values.get(0).getType() == null) || (values.get(0).getType().isEmpty())) {
               this.setType(type);
            }
         }
      } catch (Exception e) {
         logger.error("! error trying to initialize value: ", e);
      }
      return true;
   }


   /**
    * Adds new Values (i.e. instances of class) to the property. Function appends all values stored within the passed
    * property.
    *
    * @param property
    * {@link Property}: the property to append.
    */
   public void addValue(Property property) {
      for (int i = 0; i < property.valueCount(); i++) {
         Value val = property.getWholeValue(i);
         val.setAssociatedProperty(this);
         this.values.add(val);
      }
   }


   /**
    * Set the value = value-content of this property. Overwrites old content. Only works for properties with a single
    * value.
    *
    * @param value
    * {@link Object}; the new value = value-content.
    * @return {@link Boolean} true if operation succeeded, false if more than on value = value-content is stored.
    */
   public boolean setValue(Object value) {
      if (this.values.size() > 1) {
         logger
               .error("! property has more than one value > index must be specified to know which one shall be set");
         return false;
      }
      return setValueAt(value, 0);
   }


   /**
    * Set the value = value-content at a specific position in the values vector.
    *
    * @param value
    * {@link Object} the value = value-content
    * @param index
    * {@link Integer} the index
    * @return {@link Boolean} true if operation was successful, flase otherwise e.g. in case the index is out of
    * bounds.
    */
   public boolean setValueAt(Object value, int index) {
      if (index < 0 || index >= this.valueCount()) {
         logger.error("Property.setValueAt: specified index out of range!");
         return false;
      }
      try {
         Value toAdd = new Value(value, null);
         this.values.set(index, toAdd);
         logger.info("Property.setValueAt: successfully set value at index " + index);
      } catch (Exception e) {
         logger.error("Property.setValueAt: An exception occurred! ", e);
      }
      if (this.name.equalsIgnoreCase("name") && value instanceof String) {
         this.parentSection.setName((String) value);
      }
      return true;
   }


   /**
    * Get the number of stored values.
    *
    * @return {@link Integer}: the number of values stored in this property.
    */
   public int valueCount() {
      return this.values.size();
   }


   /**
    * Get the index of a certain value.
    *
    * @param value
    * {@link Value}: the value of which the index should be returned.
    * @return {@link Integer}: the values index or -1 if the value does not exist.
    */
   public int getValueIndex(Object value) {
      for (int i = 0; i < values.size(); i++) {
         if (values.get(i).getContent().equals(value))
            return i;
      }
      logger.error("! can't find index of specified value!");
      return -1;
   }


   /**
    * Get the index of a value. Searches forward starting at index.
    *
    * @param value
    * {@link Value} the value to look for.
    * @param index
    * {@link Integer}: the start index of the search.
    * @return {@link Integer}: the index or -1 if the value was not found.
    */
   public int getValueIndex(Object value, int index) {
      for (int i = index; i < values.size(); i++) {
         if (values.get(i).getContent().equals(value))
            return i;
      }
      logger.error("! can't find index of specified value!");
      return -1;
   }


   /**
    * Get the propertie's values
    *
    * @return - {@link Vector} the value stored in this property.
    */
   public Vector<Object> getValues() {
      Vector<Object> toReturn = new Vector<Object>();
      for (int i = 0; i < this.values.size(); i++) {
         toReturn.add(this.values.get(i).getContent());
      }
      return toReturn;
   }


   /**
    * Returns the first value
    *
    * @return {@link Object} the first value.
    */
   public Object getValue() {
      return getValue(0);
   }


   /**
    * Get a certain value = value-content identified by its index.
    *
    * @param index
    * {@link Integer}: the index of the value.
    * @return {@link Object}: the value or null if the index is out of bounds.
    */
   public Object getValue(int index) {
      try {
         return this.values.get(index).getContent();
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Returns the first whole value, meaning the class Value
    *
    * @return {@link Value} the first value.
    */
   public Value getWholeValue() {
      return getWholeValue(0);
   }


   /**
    * Get a certain value of class Value (meaning with all it's details) identified by its index.
    *
    * @param index
    * {@link Integer}: the index of the value.
    * @return {@link Value}: the value or null if the index is out of bounds.
    */
   public Value getWholeValue(int index) {
      try {
         return this.values.get(index);
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Removes a certain value from this property.
    *
    * @param value
    * {@link Value}: the value to be deleted.
    * @return {@link Boolean} true if removal was successful, false if not.
    */
   public boolean removeValue(Object value) {
      if (value == null) {
         logger.error("! value for removal must not be null!");
         return false;
      }
      int index = -1;
      for (int i = 0; i < this.values.size(); i++) {
         index = getValueIndex(value);
      }
      if (index < 0) {
         logger.error("! value for removal not existing!");
         return false;
      }
      this.values.remove(index);
      return true;
   }


   /**
    * Removes the value at the specified index from the property.
    *
    * @param index
    * {@link Integer}: the index.
    * @return {@link Boolean}: true if removal was successful, false if index out of bounds.
    */
   public boolean removeValue(int index) {
      if (this.values.size() <= index) {
         logger.error("! specified index for removing value out of range!");
         return false;
      }
      if (index < 0) {
         logger.error("! specified index for removing value mut be greater than 0 !");
         return false;
      }
      this.values.remove(index);
      return true;
   }


   public void removeEmptyValues() {
      for (int i = valueCount() - 1; i >= 0; i--) {
         if (getWholeValue(i).isEmpty()) {
            removeValue(i);
         }
      }
   }


   /**
    * Returns whether or not this Property is empty. A Property is empty if
    * it does not have any values or if all existing values have no content.
    *  
    * @return boolean
    */
   public boolean isEmpty() {
      boolean isEmpty = true;
      if (valueCount() == 0)
         return isEmpty;
      else {
         for (int i = 0; i < valueCount(); i++) {
            isEmpty = isEmpty & getWholeValue(i).isEmpty();
         }
      }
      return isEmpty;
   }


   /**
    * Sets the value reference of this property.
    *
    * @param reference
    * {@link String}: the definition.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   public boolean setValueReference(String reference) {
      if (this.values.size() > 1) {
         logger
               .error("! property has more than one value > index must be specified to know which id to set!");
         return false;
      }
      setValueReferenceAt(reference, 0);
      return true;
   }


   /**
    * Sets the value id of this property.
    *
    * Function is marked @deprecated and will be removed!
    * @param id
    * {@link String}: the definition.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    * 
    */
   @Deprecated
   public boolean setValueId(String id) {
      return setValueReferenceAt(id, 0);
   }


   /**
    * Set the value id of a value specified by its index. Overwrites old information.
    *
    * Function in marked @deprecated and will be removed in future versions.
    * 
    * @param id
    * {@link String}: the new value id.
    * @param index
    * {@link Integer}: the index of the value to which the id belongs.
    * @return {@link Boolean}: true if new id was set, false if index out of bounds.
    */
   @Deprecated
   public boolean setValueIdAt(String id, int index) {
      return setValueReferenceAt(id, index);
   }


   /**
    * Set the value reference of a value specified by its index. Overwrites old information.
    *
    * @param reference
    * {@link String}: the new value id.
    * @param index
    * {@link Integer}: the index of the value to which the id belongs.
    * @return {@link Boolean}: true if new id was set, false if index out of bounds.
    */
   public boolean setValueReferenceAt(String reference, int index) {
      if (this.values.size() <= index || index < 0) {
         logger.error("! specified index for settingValueId out of range!");
         return false;
      }
      this.values.get(index).setReference(reference);
      return true;
   }


   /**
    * Get the value ids.
    * Funciton is marked @deprecated and will be removed.
    * 
    * @return {@link Vector} of Strings. the values. This vector contains empty strings if no ids stored.
    */
   @Deprecated
   public Vector<String> getValueIds() {
      return getValueReferences();
   }


   /**
    * Get the value references.
    *
    * @return {@link Vector} of Strings. the values. This vector contains empty strings if no references stored.
    */
   public Vector<String> getValueReferences() {
      Vector<String> toReturn = new Vector<String>();
      for (int i = 0; i < values.size(); i++) {
         toReturn.add(values.get(i).getReference());
      }
      return toReturn;
   }


   /**
    * Get the id stored for a certain value identified by the value's index.
    *
    * @param index
    * {@link Integer}: the value's index.
    * @return {@link String}: the id stored for the value, an empty string in none stored or null if the index is out
    * of bounds.
    */
   public String getValueReference(int index) {
      try {
         String reference = this.values.get(index).getReference();
         if (reference != null && reference.isEmpty()) {
            logger.error("! no id stored for given value specified by its index!");
            return null;
         }
         return reference;
      } catch (Exception e) {
         logger.error("! fetching id for given value specified by it's index failed: ", e);
         return null;
      }
   }


   /**
    * Sets the Error estimate of this value.
    *
    * @param uncertainty
    * {@link Object} the uncertainty of the first value.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   public boolean setValueUncertainty(Object uncertainty) {
      if (this.values.size() > 1) {
         logger
               .error("! property has more than one value > index must be specified to know which uncertainty to set!");
         return false;
      }
      return setValueUncertaintyAt(uncertainty, 0);
   }


   /**
    * Set the error of a value specified by its index. Overwrites old information.
    *
    * @param uncertainty
    * {@link Object}: the new error estimate.
    * @param index
    * {@link Integer}: the index of the value to which the error belongs.
    * @return {@link Boolean}: true if new error was set, false if index out of bounds.
    */
   public boolean setValueUncertaintyAt(Object uncertainty, int index) {
      if (this.values.size() <= index || index < 0) {
         logger.error("! given index for setting uncertainty out of range!");
         return false;
      }
      this.values.get(index).setUncertainty(uncertainty);
      return true;
   }


   /**
    * Get the uncertainties stored in this property.
    *
    * @return - {@link Vector} the value stored in this property.
    */
   public Vector<Object> getValueUncertainties() {
      Vector<Object> toReturn = new Vector<Object>();
      for (int i = 0; i < this.values.size(); i++) {
         toReturn.add(this.values.get(i).getUncertainty());
      }
      return toReturn;
   }


   /**
    * Get the uncertainty estimation stored for the value. Only working if there is only one value!
    *
    * @return {@link Object}: the according uncertainty value if existing. An empty String if no error stored or null
    * if there are more values existing
    */
   public Object getValueUncertainty() {
      if (values.size() > 1) {
         logger.error("! more than one value existing > index of value must be specified to know "
               + "which uncertainty shall be returned!");
         return null;
      }
      return getValueUncertainty(0);
   }


   /**
    * Get the uncertainty estimation stored for a certain value defined by its index.
    *
    * @param index
    * {@link Integer}: the index in the values vector.
    * @return {@link Object}: the according uncertainty value if existing. An empty String if no error stored or null
    * if the index is out of range.
    */
   public Object getValueUncertainty(int index) {
      try {
         Object unctr = this.values.get(index).getUncertainty();
         if (unctr == null) {
            return null;
         } else if (unctr.toString().isEmpty()) {
            return null;
         } else {
            return unctr;
         }
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Sets the value definition of this property.
    *
    * @param definition
    * {@link String}: the definition.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   public boolean setValueDefinition(String definition) {
      if (this.values.size() > 1) {
         logger
               .error("! property has more than one value > index must be specified to know which valueComment to set!");
         return false;
      }
      return setValueDefinitionAt(definition, 0);
   }


   /**
    * Set the value definition of a value specified by its index. Overwrites old information.
    *
    * @param definition
    * {@link String}: the new value definition.
    * @param index
    * {@link Integer}: the index of the value to which the definition belongs.
    * @return {@link Boolean}: true if new definition was set, false if index out of bounds.
    */
   public boolean setValueDefinitionAt(String definition, int index) {
      if (this.values.size() <= index || index < 0) {
         logger.error("! given index for setting valueComment out of range!");
         return false;
      }
      this.values.get(index).setDefinition(definition);
      return true;
   }


   /**
    * Get the value Definitions.
    *
    * @return {@link Vector} of Strings.
    */
   public Vector<String> getValueDefinitions() {
      Vector<String> toReturn = new Vector<String>();
      for (int i = 0; i < values.size(); i++) {
         toReturn.add(values.get(i).getDefinition());
      }
      return toReturn;
   }


   /**
    * Get the definition of a certain value identified by its index.
    *
    * @param index
    * {@link Integer} the index of the value, respectively its defintion.
    * @return {@link String}: the value comment if stored, an empty string if not or null if the index is out of
    * bounds.
    */
   public String getValueDefinition(int index) {
      try {
         String comment = this.values.get(index).getDefinition();
         if (comment != null && comment.isEmpty()) {
            // logger.error("! no valueComment found for value specified by it's index!");
            return null;
         }
         return comment;
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Merges this property with another one. Generally, all values and other information of the other property will be
    * copied. The way the merging is done in case of conflict can be set by the mergeOption parameter which can assume
    * the following values:
    * <ol>
    * <li>MERGE_THIS_OVERRIDES_OTHER: the local information overrides the information passed in otherProperty.</li>
    * <li>MERGE_OTHER_OVERRIDES_THIS: the global information passed with otherProperty overrides local information.</li>
    * <li>MERGE_COMBINE: the local information will be combined with the passed property. values with the same name
    * will be fused.</li>
    * </ol>
    *
    * @param otherProperty
    * {@link Property} the other Property which shall be merged with this property.
    * @param mergeOption
    * {@link Integer} the way merging is done.
    *
    */
   public void merge(Property otherProperty, int mergeOption) {
      if (!this.name.equalsIgnoreCase(otherProperty.getName())) {
         logger.error("Property.merge error: cannot merge properties of differnt names!");
         return;
      }
      if ((this.getType() != null && otherProperty.getType() != null)
            && !this.getType().equalsIgnoreCase(otherProperty.getType())) {
         logger
               .error("Property.merge error: cannot merge properties based with different datatypes!");
         return;
      }
      if ((this.getMapping() != null && otherProperty.getMapping() != null)
            && !this.getMapping().sameFile(otherProperty.getMapping())) {
         logger
               .error("Property.merge error: cannot merge properties mapping to different properties!");
         return;
      }
      if ((this.getDefinition() != null && otherProperty.getDefinition() != null)
            && !this.getDefinition().equalsIgnoreCase(otherProperty.getDefinition())) {
         logger
               .error("Property.merge error: cannot merge properties having different nameDefinitions!");
         return;
      }
      if ((this.getUnit(0) != null && otherProperty.getUnit(0) != null)
            && !this.getUnit(0).equalsIgnoreCase(otherProperty.getUnit(0))) {
         logger
               .error("Property.merge error: cannot merge properties having different units! Maybe the next version can...");
         return;
      }
      // actually merge: first the easy ones, i.e. those that can occur only once
      if (this.getDefinition() == null) {
         this.setDefinition(otherProperty.getDefinition());
      }
      if (this.getType() == null) {
         this.setType(otherProperty.getType());
      }
      if (this.getUnit(0) == null) {
         this.setUnit(otherProperty.getUnit(0));
      }
      if (this.getMapping() == null) {
         this.setMapping(otherProperty.getMapping());
      }
      if (this.getDependency() == null) {
         this.setDependency(otherProperty.getDependency());
      } else {
         if (mergeOption == Section.MERGE_OTHER_OVERRIDES_THIS
               && (otherProperty.getDependency() != null || otherProperty.getDependency().isEmpty())) {
            this.setDependency(otherProperty.getDependency());
         }
      }
      if (this.getDependencyValue() == null) {
         this.setDependencyValue(otherProperty.getDependencyValue());
      } else {
         if (mergeOption == Section.MERGE_OTHER_OVERRIDES_THIS
               && (otherProperty.getDependencyValue() != null || otherProperty.getDependency()
                     .isEmpty())) {
            this.setDependency(otherProperty.getDependency());
         }
      }
      for (int i = 0; i < otherProperty.valueCount(); i++) {
         if (this.values.contains(otherProperty.getValue(i))) {
            mergeValue(this.values.indexOf(otherProperty.getValue(i)), otherProperty, i,
                  mergeOption);
         } else {
            if (mergeOption == Section.MERGE_COMBINE) {
               this.addValue(otherProperty.getValue(i), otherProperty.getValueReference(i),
                     otherProperty
                           .getValueUncertainty(i), otherProperty.getFilename(i), otherProperty
                           .getValueDefinition(i));
            } else if (mergeOption == Section.MERGE_OTHER_OVERRIDES_THIS && this.valueCount() == 1) {
               setValueAt(otherProperty.getValue(), i);
               mergeValue(this.values.indexOf(otherProperty.getValue(i)), otherProperty, i,
                     mergeOption);
            }
         }
      }
   }


   /**
    * Validates this {@link Property} against the definition in a terminology.
    * Method will cause logger Warnings in case validation did not succeed.
    * 
    * @param terminologyProperty The {@link Property} as it is defined in the terminology.
    */
   public void validate(Property terminologyProperty) {
      if (definition != null && !definition.isEmpty()) {
         if (!this.definition.equalsIgnoreCase(terminologyProperty.getDefinition())) {
            logger
                  .warn("Property: "
                        + this.getName()
                        + "contains a 'definition' that differs from terminology! Kept original definition!");
         }
      }
      if (terminologyProperty.getDependency() != null
            && !terminologyProperty.getDependency().isEmpty()) {
         if (this.getParent() != null
               && !this.getParent().containsProperty(terminologyProperty.getDependency())) {
            logger.warn("Validation error on Property: " + this.getParent().getPath() + "#"
                  + this.getName() + "! \n Terminology requests a sibling property with the name: "
                  +
                  terminologyProperty.getName() + " which was not found!");
         } else {
            if (terminologyProperty.getDependencyValue() != null
                  && !terminologyProperty.getDependencyValue().isEmpty()) {
               String terminolgyValue = terminologyProperty.getDependencyValue();
               Property dependencyProperty = getParent().getProperty(
                     terminologyProperty.getDependency());
               Vector<Object> values = dependencyProperty.getValues();
               Iterator<Object> iter = values.iterator();
               boolean match = false;
               while (iter.hasNext()) {
                  if (iter.next().toString().equalsIgnoreCase(terminolgyValue)) {
                     match = true;
                     break;
                  }
               }
               if (!match)
                  logger.warn("Validation error on Property: " + this.getParent().getPath()
                        + "#" + this.getName()
                        + "! \n Terminology requests a sibling property with the name: " +
                        terminologyProperty.getName() + " that contains the value: "
                        + dependencyValue + "! No match was found!");
            }
         }
      }
      for (int i = 0; i < valueCount(); i++) {
         values.get(i).validate(terminologyProperty);
      }
   }


   @Deprecated
   /**
    * This function is marked @deprecated and will be removed in future versions. Use validate instead! 
    */
   public void compareToTerminology(Property termProp) {
      validate(termProp);
   }


   /**
    * Merges a value of a property and all its appendant elements. Again, information that exists in only one of the
    * properties are accepted irrespective of the mergeOption. Only if conflicts arise the mergeOption is relevant.
    *
    * @param thisValueIndex
    * {@link Integer} this properties value;
    * @param otherProperty
    * {@link Property} the property with which this should be merged
    * @param otherValueIndex
    * {@link Integer} the value index in the otherProperty
    * @param mergeOption
    * {@link Integer} defines the behavior if a conflict arises, see {@linkplain Property.merge}
    */
   private void mergeValue(int thisValueIndex, Property otherProperty, int otherValueIndex,
                           int mergeOption) {
      switch (mergeOption) {
         case Section.MERGE_THIS_OVERRIDES_OTHER:
            if (this.getValueDefinition(thisValueIndex) == null) {
               this.setValueDefinitionAt(otherProperty.getValueDefinition(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueUncertainty(thisValueIndex) == null) {
               this.setValueUncertaintyAt(otherProperty.getValueUncertainty(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueFilename(thisValueIndex) == null) {
               this.setValueFilenameAt(otherProperty.getValueFilename(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueReference(thisValueIndex) == null) {
               this.setValueReferenceAt(otherProperty.getValueReference(otherValueIndex),
                     thisValueIndex);
            }
            break;
         case Section.MERGE_OTHER_OVERRIDES_THIS:
            if (this.getValueDefinition(thisValueIndex) == null) {
               this.setValueDefinitionAt(otherProperty.getValueDefinition(otherValueIndex),
                     thisValueIndex);
            } else if (otherProperty.getValueDefinition(otherValueIndex) != null) {
               this.setValueDefinitionAt(otherProperty.getValueDefinition(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueUncertainty(thisValueIndex) == null) {
               this.setValueUncertaintyAt(otherProperty.getValueUncertainty(otherValueIndex),
                     thisValueIndex);
            } else if (otherProperty.getValueUncertainty(otherValueIndex) != null) {
               this.setValueUncertaintyAt(otherProperty.getValueUncertainty(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueFilename(thisValueIndex) == null) {
               this.setValueFilenameAt(otherProperty.getValueFilename(otherValueIndex),
                     thisValueIndex);
            } else if (otherProperty.getValueFilename(otherValueIndex) != null) {
               this.setValueFilenameAt(otherProperty.getValueFilename(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueReference(thisValueIndex) == null) {
               this.setValueReferenceAt(otherProperty.getValueReference(otherValueIndex),
                     thisValueIndex);
            } else if (otherProperty.getValueReference(otherValueIndex) != null) {
               this.setValueReferenceAt(otherProperty.getValueReference(otherValueIndex),
                     thisValueIndex);
            }
            break;
         case Section.MERGE_COMBINE:
            if (this.getValueDefinition(thisValueIndex) == null) {
               this.setValueDefinitionAt(otherProperty.getValueDefinition(otherValueIndex),
                     thisValueIndex);
            } else if (otherProperty.getValueDefinition(otherValueIndex) != null) {
               this.setValueDefinitionAt(this.getValueDefinition(thisValueIndex) + "\n"
                     + otherProperty.getValueDefinition(otherValueIndex), thisValueIndex);
            }
            if (this.getValueUncertainty(thisValueIndex) == null) {
               this.setValueUncertaintyAt(otherProperty.getValueUncertainty(otherValueIndex),
                     thisValueIndex);
            }
            if (this.getValueFilename(thisValueIndex) == null) {
               this
                     .setValueFilenameAt(otherProperty.getValueFilename(otherValueIndex),
                           thisValueIndex);
            }
            if (this.getValueReference(thisValueIndex) == null) {
               this.setValueReferenceAt(otherProperty.getValueReference(otherValueIndex),
                     thisValueIndex);
            }
            break;
      }
   }


   /**
    * Checking name and if necessary converting to CamelCase leading characters that are not alphabetical get P_ at the
    * beginning
    *
    * @param name
    * {@link String} the requested property name
    * @return {@link String} the converted name or the original if all is ok.
    */
   public static String checkNameStyle(String name) {
      name = name.trim();
      while (name.contains(" ")) {
         name = name.substring(0, name.indexOf(" "))
               + name.substring(name.indexOf(" ") + 1, name.indexOf(" ") + 2).toUpperCase()
               + name.substring(name.indexOf(" ") + 2);
         logger.warn("Invalid property name:\tgenerating CamelCase by removing blanks");
      }

      String nameRegex = "^[a-zA-Z]{1}.*"; // checking beginning: normal letter, than anything
      if (!name.matches(nameRegex)) {
         name = "P_" + name;
         logger.warn("Invalid property name:\t'p_' added as no leading character found");
      }
      return name;
   }


   /**
    * Creates a copy of this property.
    *
    * @return {@link Property} the copy.
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public Property copy() throws IOException, ClassNotFoundException {
      File tempFile = File.createTempFile("property", ".ser");
      tempFile.deleteOnExit();
      ObjectOutputStream objOut = new ObjectOutputStream(new BufferedOutputStream(
            new FileOutputStream(tempFile)));
      objOut.writeObject(this);
      objOut.close();

      ObjectInputStream objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
            tempFile)));
      Property copy = (Property) objIn.readObject();
      objIn.close();
      return copy;
   }


   /**
    * Returns the property as an {@link Vector} of Objects. If there is more than one value, the first is returned.
    * "name","value"
    *
    * @return Object[] the content of this property.
    */
   public Vector<Object> getPropertyAsVector() {
      Vector<Object> propertyVector = new Vector<Object>();
      propertyVector.add(name);
      propertyVector.add(values.get(0).getReference());
      propertyVector.add(values.get(0).getContent());
      propertyVector.add(values.get(0).getUncertainty());
      propertyVector.add(values.get(0).getUnit());
      propertyVector.add(values.get(0).getType());
      propertyVector.add(values.get(0).getFilename());
      propertyVector.add(values.get(0).getDefinition());
      propertyVector.add(definition);
      propertyVector.add(dependency);
      propertyVector.add(dependencyValue);
      propertyVector.add(mappingURL);
      return propertyVector;
   }


   /**
    * Returns the property as an Vector of Objects. The index of the value must be given.
    *
    * @param index
    * {@link Integer}: the index of the selected value.
    * @return Vector<Object> the content of this property and the specified value. If index out of bounds null is
    * returned.
    */
   public Vector<Object> getPropertyAsVector(int index) {
      if (index >= values.size()) {
         return null;
      }
      try {
         Vector<Object> propertyVector = new Vector<Object>();
         propertyVector.add(name);
         propertyVector.add(values.get(index).getReference());
         propertyVector.add(values.get(index).getContent());
         propertyVector.add(values.get(index).getUncertainty());
         propertyVector.add(values.get(index).getUnit());
         propertyVector.add(values.get(index).getType());
         propertyVector.add(values.get(index).getFilename());
         propertyVector.add(values.get(index).getDefinition());
         propertyVector.add(definition);
         propertyVector.add(dependency);
         propertyVector.add(dependencyValue);
         propertyVector.add(mappingURL);
         return propertyVector;
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Get the property's unit. If there is more than one value the index must be specified to know which unit shall be
    * returned, otherwise null. units can differ like the following example: sec, millisec, nanosec
    *
    * @return {@link String}
    */
   public String getUnit() {
      if (values.size() > 1) {
         logger.error("! property '" + this.getName()
               + "' has more than one value > index must be specified to know which unit shall be"
               + " returned!");
         return null;
      }
      return getUnit(0);
   }


   /**
    * Get the unit of the propertie's value specified via an index.
    *
    * @return {@link String}
    */
   public String getUnit(int index) {
      return values.get(index).getUnit();
   }


   /**
    * Get the value's data type. As all types must be the same, type of first value is returned.
    *
    * @return {@link String}
    */
   public String getType() {
      return this.values.get(0).getType();
   }


   /**
    * Function is marked @deprecated and will be removed. Use getValueFilename() instead!   
    */
   @Deprecated
   public String getFilename() {
      return getValueFilename(0);
   }


   /**
    * Function is marked @deprecated and will be removed. Use getValueFilename(int index) instead!   
    */
   @Deprecated
   public String getFilename(int index) {
      return getValueFilename(index);
   }


   /**
    * Returns the mimetype of the property. Only working if not more than one value existing!
    *
    * @return {@link String}: returns the mimetype of a certain propery. Returns an empty String if no mimeType stored
    * or null if the index is out of range.
    */
   public String getValueFilename() {
      if (values.size() > 1) {
         logger
               .error("! property has more than one value > index for returning filename must be specified");
         return null;
      }
      return getValueFilename(0);
   }


   /**
    * Returns the filename information stored in the specified value defined by the index.
    *
    * @param index
    * {@link Integer}: the index to specify which filename shall be returned (i.e. from which value)
    * @return {@link String}: the filename or null if empty.
    */
   public String getValueFilename(int index) {
      try {
         String filename = this.values.get(index).getFilename();
         if (filename != null && filename.isEmpty()) {
            return null;
         }
         return filename;
      } catch (Exception e) {
         logger.error("", e);
         return null;
      }
   }


   /**
    * Returns the checksum of the first value stored in this porperty.
    * 
    * @return String the checksum or null if no checksum is stored or index out of bounds.
    */
   public String getValueChecksum() {
      return getValueChecksum(0);
   }


   /**
    * Returns the checksum stored in the specified value.
    * 
    * @param index int: the index of the value.
    * @return String the checksum or null if no checksum is stored or index out of bounds.
    */
   public String getValueChecksum(int index) {
      if (index > -1 && index < valueCount())
         return getWholeValue(index).getChecksum();
      else
         return null;
   }


   /**
    * Returns the encoder of the first value. 
    * @return a {@link String} indicating the encoder or empty string.  
    */
   public String getValueEncoder() {
      return getValueEncoder(0);
   }


   /**
    * Returns the encoder of the value specfied by the index.
    * @param index int.
    * @return a {@link String} indicating the encoder or empty string.
    */
   public String getValueEncoder(int index) {
      if (index > -1 && index < valueCount())
         return getWholeValue(index).getEncoder();
      else
         return "";
   }


   /**
    * Set the mapping of this {@link Property}. The mapping is specified 
    * in form of an {@link URL} (e.g. http://portal.g-node-org/odml/terminologies/sectionType#newPoperty).
    * The anchor part may be missing. In this case the property will be copied into a destination
    * section of the specified type.
    * 
    * @param mappingURL an {@link URL} defining  
    */
   public void setMapping(URL mappingURL) {
      this.mappingURL = mappingURL;
   }


   /**
    * Sets the mapping information for this property.
    *
    * @param mappingURL
    */
   public void setMapping(String mappingURL) {
      URL url = null;
      try {
         url = new URL(mappingURL);
      } catch (MalformedURLException m) {
         logger.error("Property.setPropertyMapping: ", m);
      }
      this.setMapping(url);
   }


   /**
    * Removes the mapping information from this property. The parent mapping, however, is unaffected.
    */
   public void removePropertyMapping() {
      this.mappingURL = null;
   }


   /**
    * Careful! Sets all units of this property (i.e. of all values) to the same content! Overwrites old content!
    *
    * @param unit
    * {@link String} the new unit.
    */
   public void setUnit(String unit) {
      if (valueCount() > 1)
         logger
               .warn("You ask me to set the unit but there are many values. Changed the units for all values!");
      for (int i = 0; i < values.size(); i++) {
         values.get(i).setUnit(unit);
      }
   }


   /**
    * Returns the mapping stored within this property.
    *
    * @return {@link URL} the mapping information related to this property or null if none found.
    */
   public URL getMapping() {
      return this.mappingURL;
   }


   /**
    * Setting the unit of the value specified by an index to the new unit; Overwrites old content!
    *
    * @param unit
    * {@link String} the new unit.
    */
   public void setUnitAt(String unit, int index) {
      values.get(index).setUnit(unit);
   }


   /**
    * Careful! Sets the datatype of this property. Overwrites old content!
    *
    * @param type
    * {@link String}: the type of data represented by this property.
    */
   public void setType(String type) {
      for (int i = 0; i < values.size(); i++) {
         values.get(i).setType(type);
      }
   }


   /**
    * Set the parent of this property. Parents indicate that this property is only meaningful once the parent property
    * is specified.Overwirtes old content!
    *
    * @param dependency
    * {@link String}: the parent of this property
    */
   public void setDependency(String dependency) {
      if (dependency != null)
         this.dependency = dependency;
      else
         this.dependency = "";
   }


   /**
    * Set the value of the parent property that makes this property meaningful. Overwirtes old content!
    *
    * @param dependencyValue
    * {@link String}: the parentValue.
    */
   public void setDependencyValue(String dependencyValue) {
      if (dependencyValue != null)
         this.dependencyValue = dependencyValue;
      else
         this.dependencyValue = "";
   }


   /**
    * Set the reference of this property. Only working if not more than one value existing!
    *
    * @param reference
    * {@link String}: The identifier of the property.
    * @return {@link Boolean} true if operation succeeded, false if more than one value existing
    */
   public boolean setReference(String reference) {
      if (this.values.size() > 1) {
         logger
               .error("! Property has more than one value > index must be specified to know which reference shall be set!");
         return false;
      }
      return setReferenceAt(reference, 0);
   }


   public boolean setReferenceAt(String reference, int index) {
      this.values.get(index).setReference(reference);
      return true;
   }


   /**
    * Sets the value mimetype of this property.
    *
    * @param filename
    * {@link String}: the definition.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   @Deprecated
   public boolean setDefaultFileName(String filename) {
      return setValueFilename(filename);
   }


   /**
    * Sets filename associated with the first value of this property.
    *
    * Function is marked @deprecated and will be removed.
    * 
    * @param filename {@link String}: The filename.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   @Deprecated
   public boolean setFilename(String filename) {
      return setValueFilenameAt(filename, 0);
   }


   /**
    * Sets filename associated with the first value of this property.
    *
    * @param filename {@link String}: The filename.
    * @return {@link Boolean} true if operation succeeded, false if there is more than a single value stored in this
    * property.
    */
   public boolean setValueFilename(String filename) {
      return setValueFilenameAt(filename, 0);
   }


   /**
    * Set the value filename of a value specified by its index. Overwrites old information.
    *
    * @param filename
    * {@link String}: the default file name which should be used when saving the object.
    * @param index
    * {@link Integer}: the index of the value to which the filename belongs.
    * @return {@link Boolean}: true if new name was set, false if index out of bounds.
    * 
    * Function marked as @deprecated use setValueFilenameAt instead
    */
   @Deprecated
   public boolean setDefaultFileNameAt(String filename, int index) {
      if (this.values.size() <= index || index < 0) {
         logger.error("! index of value for setting filename out of range!");
         return false;
      }
      if (!this.values.get(0).getType().equalsIgnoreCase("binary")) {
         logger.error("! type of property must be binary if filename shall be set!");
         return false;
      }
      this.values.get(index).setFilename(filename);
      return true;
   }


   /**
    * Set the filename associated with the binary content of value specified by index. 
    * Overwrites old information.
    *
    * @param filename
    * {@link String}: the default file name which should be used when saving the object.
    * @param index
    * {@link Integer}: the index of the value to which the filename belongs.
    * @return {@link Boolean}: true if new name was set, false if index out of bounds.
    * 
    */
   public boolean setValueFilenameAt(String filename, int index) {
      if (this.values.size() <= index || index < 0) {
         logger.error("! index of value for setting filename out of range!");
         return false;
      }
      if (!this.values.get(0).getType().equalsIgnoreCase("binary")) {
         logger.error("! type of property must be binary if filename shall be set!");
         return false;
      }
      this.values.get(index).setFilename(filename);
      return true;
   }


   /**
    * Function to convert the content of the indicated file to an array of bytes. Is primarily for internal use to
    * Base64 encode binary data.
    *
    * @param file
    * {@link File}: the file to convert.
    * @return byte[]: the array of bytes contained in the file.
    * @throws IOException
    */
   public static byte[] getBytesFromFile(File file) throws IOException {
      return Value.getBytesFromFile(file);
   }


   /**
    * Writes the value of this property (if of type binary) to disc. In case there is more than one value stored, the
    * first is written to disc.
    *
    * @param filename
    * {@link String}: the full path of the new file.
    */
   public void writeBinaryToDisc(String filename) throws Exception {

      File outFile = new File(filename);
      if (outFile.isDirectory()) {
         if (values.get(0).getFilename().isEmpty()) {
            throw new Exception(
                  "Property does not define a default file name. Please provide a full file name.");
         }
         outFile = new File(filename + values.get(0).getFilename());
      }
      writeBinary(outFile, 0);
   }


   /**
    * Writes the identified value of this property (if of type binary) to disc.
    *
    * @param filename
    * {@link String}: the full path of the new file.
    * @param index
    * {@link Integer}: the value index.
    */
   public void writeBinaryToDisc(String filename, int index) {
      try {
         File outFile = new File(filename);
         writeBinary(outFile, index);
      } catch (Exception e) {
         logger.error("could not create File from string: " + filename, e);
      }
   }


   /**
    * Writes the value of this property (if of type binary) to disc. In case there is more than one value stored, the
    * first is written to disc.
    *
    * @param fileUrl
    * {@link URL}: the URL of the file.
    */
   public void writeBinaryToDisc(URL fileUrl) {
      try {
         File outFile = new File(fileUrl.toURI());
         writeBinary(outFile, 0);
      } catch (Exception e) {
         logger.error("could not create File from URL: " + fileUrl, e);
      }
   }


   /**
    * Writes the identified value of this property (if of type binary) to disc.
    *
    * @param fileUrl
    * {@link URL}: the URL of the file.
    * @param index
    * {@link Integer}: the value index.
    */
   public void writeBinaryToDisc(URL fileUrl, int index) {
      try {
         File outFile = new File(fileUrl.toURI());
         writeBinary(outFile, index);
      } catch (Exception e) {
         logger.error("could not create File from URL: " + fileUrl, e);
      }
   }


   /**
    * Writes the value of this property (if of type binary) to disc. In case there is more than one value stored, the
    * first is written to disc.
    *
    * @param fileUri
    * {@link URI}: THe file URI.
    */
   public void writeBinaryToDisc(URI fileUri) {
      try {
         File outFile = new File(fileUri);
         writeBinary(outFile, 0);
      } catch (Exception e) {
         logger.error("could not create File from the specified URI: " + fileUri, e);
      }
   }


   /**
    * Write the content of the given property value to disc
    *
    * @param fileUri
    * {@link URI}: THe file URI.
    * @param index
    * {@link Integer}: the value index.
    */
   public void writeBinaryToDisc(URI fileUri, int index) {
      try {
         File outFile = new File(fileUri);
         writeBinary(outFile, index);
      } catch (Exception e) {
         logger.error("could not create File from the specified URI: " + fileUri, e);
      }
   }


   /**
    * Write the property content to disc. Function calls public method writeBinaryToDisc.
    *
    * @param outFile
    * {@link File}: the File to which the content has to be written.
    * @param index
    * {@link Integer}: the index of the value.
    */
   private void writeBinary(File outFile, int index) throws Exception {
      if (!this.values.get(0).getType().equalsIgnoreCase("binary")) {
         logger.error("Property value is not of type binary!");
         return;
      }
      if (index < 0) {
         logger.error("!index specified for writing value to disc out of range!");
         return;
      } else if (index > values.size() - 1) {
         logger.error("!index specified for writing value to disc out of range!");
         return;
      }
      if (outFile.exists()) {
         throw new Exception("File already exists please provide a different file name.");
      }
      Value.writeBinaryToDisc(values.get(index).getContent().toString(), outFile);
   }


   /**
    * Returns a String representation of this section. I.e the fact that it is a section and it's name.
    */
   @Override
   public String toString() {
      String info = (this.name);
      return info;
   }


   /**
    * Returns an extended String representation of this section. I.e it's name, level, completePath, number and names
    * of subsections and number and names of appended properties
    */
   public String toStringExtended() {
      String info = ("property '" + this.name + "'; completePath: ");
      info += (this.getParent()).getPath();
      info += ("/" + this.name);
      return info;
   }


   /**
    * Compares this property's value to the given object. Returns
    *
    * @param aObject
    * {@link Object} the object that
    * @param type
    * {@link String}
    * @return {@link Integer} the match level.
    * @deprecated not yet implemented!
    */
   @Deprecated
   public int match(Object aObject, String type) {
      // TODO
      return MATCH_ERROR;
   }


   /**
    * Compares this Property to another one and returns a match-level
    *
    * @param anotherProperty
    * @return {@link Integer} the match level.
    * @deprecated not implemented yet will return MATCH_ERROR
    */
   @Deprecated
   public int match(Property anotherProperty) {
      // TODO
      return MATCH_ERROR;
   }


   /**
    * Static method to match two objects
    *
    * @param anObject
    * @param type
    * @param anotherObject
    * @return {@link Integer} the match level
    */
   public static int match(Object anObject, Object anotherObject, String type) {
      if (anObject == null || anotherObject == null || type == null) {
         System.out.println("match returns error, object1, object2 or type is null.");
         return MATCH_ERROR;
      }
      if (type.isEmpty()) {
         System.out.println("match returns error, type is empty.");
         return MATCH_ERROR;
      }

      if (type.equalsIgnoreCase("person")) {
         return nameMatch(anObject.toString(), anotherObject.toString());
      } else if (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("string")) {
         if (anObject.toString().equalsIgnoreCase(anotherObject.toString())) {
            return MATCH_EXACT;
         } else {
            return MATCH_NO;
         }
      } else if (type.equalsIgnoreCase("int")) {
         try {
            if (((Integer) anObject).compareTo((Integer) anotherObject) == 0) {
               return MATCH_EXACT;

            } else {
               return MATCH_NO;
            }
         } catch (Exception e) {
            logger.error("", e);
            return MATCH_ERROR;
         }
      } else if (type.equalsIgnoreCase("float")) {
         try {
            if (((Float) anObject).compareTo((Float) anotherObject) == 0) {
               return MATCH_EXACT;
            } else {
               return MATCH_NO;
            }
         } catch (Exception e) {
            logger.error("", e);
            return MATCH_ERROR;
         }
      } else if (type.equalsIgnoreCase("date")) {
         try {
            if (((java.sql.Date) anObject).equals(anotherObject)) {
               return MATCH_EXACT;
            } else {
               return MATCH_NO;
            }
         } catch (Exception e) {
            logger.error("", e);
            return MATCH_ERROR;
         }
      } else if (type.equalsIgnoreCase("time")) {
         try {
            if (((java.sql.Time) anObject).equals(anotherObject)) {
               return MATCH_EXACT;
            } else {
               return MATCH_NO;
            }
         } catch (Exception e) {
            logger.error("", e);
            return MATCH_ERROR;
         }
      }
      return MATCH_ERROR;
   }


   /**
    * Tries to match two strings that represent names. Returns a match score between MATCH_NO (0) and MATCH_FIRST_LAST
    * (50). Method breaks down the names into first and last name component. Middle initials and second names are
    * ignored. Pure initials work only if both names are initials. If names consist of one word only it is assumed to
    * be the last name. Match levels are:
    * <ol>
    * <li><b>MATCH_ERROR:</b> if the some error occurred, e.g. if arguments are invalid.</li>
    * <li><b>MATCH_NO:</b> if no match was found.</li>
    * <li><b>MATCH_FIRST_CONFLICT_LAST_MATCH:</b> if the last name matches but the first name fails. Might be the
    * consequence of a misspelling.</li>
    * <li><b>MATCH_FIRST_OR_LAST_ONLY:</b> if at least one name has only first or last name component matching can at
    * best be a first or last name match alone.</li>
    * <li><b>MATCH_FIRST_INITIAL_LAST:</b> if the last matches but the first matches only with the initials or the
    * staring sequence. This may occur if a first name like "Jean-Marc" is passed as "Jean Marc" or with only first or
    * middle name alone.</li>
    * <li><b>MATCH_FIRST_LAST:</b> the best score, only in there are first and last name given and both are matching.
    * Some risk resides if persons are distinguished with their middle names/initials only./</li>
    *
    * </ol>
    *
    * @param name1
    * {@link String} the first name. Must not be null or empty.
    * @param name2
    * {@link String} the second name. Must not be null or empty.
    * @return {@link Integer} the matching score.
    */
   private static int nameMatch(String name1, String name2) {
      if (name1 == null || name2 == null) {
         System.out.println("nameMatch return error, one of the names is null");
         return MATCH_ERROR;
      }
      if (name1.isEmpty() || name2.isEmpty()) {
         System.out.println("nameMatch return error, one of the names is empty");
         return MATCH_ERROR;
      }
      String firstName1, firstName2, lastName1, lastName2;
      name1 = name1.trim();
      name2 = name2.trim();
      firstName1 = getFirstName(name1);
      firstName2 = getFirstName(name2);
      lastName1 = getLastName(name1);
      lastName2 = getLastName(name2);
      if (!firstName1.isEmpty() && !firstName2.isEmpty()) {
         if (firstName1.equalsIgnoreCase(firstName2) && lastName1.equalsIgnoreCase(lastName2)) {
            return MATCH_FIRST_LAST;
         } else if (!firstName1.equalsIgnoreCase(firstName2)
               && (firstName1.startsWith(firstName2) || firstName2.startsWith(firstName1))
               && lastName1.equalsIgnoreCase(lastName2)) {
            return MATCH_FIRST_INITIAL_LAST;
         } else if ((firstName1.startsWith(firstName2) && lastName1.startsWith(lastName2))
               || (firstName2.startsWith(firstName1) && lastName2.startsWith(lastName1))) {
            return MATCH_INITIALS_ONLY;
         } else if (!firstName1.equalsIgnoreCase(firstName2)
               && lastName1.equalsIgnoreCase(lastName2)) {
            return MATCH_FIRST_CONFLICT_LAST_MATCH;
         } else {
            return MATCH_NO;
         }
      } else {// if the first names of either name 1 or 2 are empty
         if (firstName1.isEmpty() && firstName2.isEmpty()) {
            if (lastName1.equalsIgnoreCase(lastName2)) {
               return MATCH_FIRST_OR_LAST_ONLY;
            } else {
               return MATCH_NO;
            }
         } else {// only one of them is missing
            if (firstName1.isEmpty()
                  && (lastName1.equalsIgnoreCase(lastName2) || lastName1
                        .equalsIgnoreCase(firstName2))) {
               return MATCH_FIRST_OR_LAST_ONLY;
            } else if (firstName2.isEmpty()
                  && (lastName2.equalsIgnoreCase(lastName1) || lastName2
                        .equalsIgnoreCase(firstName1))) {
               return MATCH_FIRST_OR_LAST_ONLY;
            } else {
               return MATCH_NO;
            }
         }
      }
   }


   public static String getLastName(String fullName) {
      if (fullName.contains(",")) {// assuming lastName first then firstName maybe as initial or with middle name
         // initial
         return fullName.substring(0, fullName.indexOf(","));
      } else if (!fullName.contains(".") && fullName.contains(" ")) {// firstName first no initial, lastName last
         return fullName.substring(fullName.lastIndexOf(" ") + 1);
      } else if (fullName.contains(".") && !fullName.contains(" ")) {// has an initial, assume that the lastName
         // behind the dot.
         return fullName.substring(fullName.lastIndexOf(".") + 1);
      } else if (!fullName.contains(",") && !fullName.contains(" ")) {// is only one word, assumes that there is only
         // a last name
         return fullName;
      } else {// firstName first, maybe as initial or with intials, lastName last
         return fullName.substring(fullName.lastIndexOf(" ") + 1);
      }
   }


   public static String getFirstName(String fullName) {
      if (fullName.contains(",")) {// assuming lastName first then firstName maybe as initial or with middle name
         // initial
         String firstName = fullName.substring(fullName.indexOf(",") + 1).trim();
         if (firstName.contains(" ")) {// maybe more than one first name, take only the first
            firstName = firstName.substring(0, firstName.indexOf(" "));
         }
         if (firstName.contains(".")) {// take only the firstName or fistName initial without the dot
            firstName = firstName.substring(0, firstName.indexOf("."));
         }
         return firstName;
      } else if (fullName.contains(".") && !fullName.contains(" ")) {// firstName initial with dot first but otherwise
         // unbroken word
         return fullName.substring(0, fullName.indexOf("."));
      } else if (!fullName.contains(".") && fullName.contains(" ")) {// firstName first no initial, lastName last
         return fullName.substring(0, fullName.indexOf(" "));
      } else if (!fullName.contains(",") && !fullName.contains(" ")) {// is only one word, assumes that there is only
         // a last name
         return "";
      } else {// firstName first, maybe as initial or with intials, lastName last
         String firstName = fullName;
         if (fullName.contains(".")) {
            firstName = fullName.substring(0, fullName.indexOf(".")).trim();
         }
         if (firstName.contains(" ")) {
            firstName = firstName.substring(0, firstName.indexOf(" "));
         }
         if (firstName.contains(".")) {// take only the firstName or fistName initial without the dot
            firstName = firstName.substring(0, firstName.indexOf("."));
         }
         return firstName;

      }
   }


   @Override
   public Enumeration<Value> children() {
      Enumeration<Value> voo = (this.values).elements();
      return voo;
   }


   @Override
   public boolean getAllowsChildren() {
      return true;
   }


   @Override
   public TreeNode getChildAt(int childIndex) {
      TreeNode value = getWholeValue(childIndex);
      return value;
   }


   @Override
   public int getChildCount() {
      return this.values.size();
   }


   @Override
   public int getIndex(TreeNode node) {
      if (node instanceof Value) {
         Value voo = (Value) node;
         for (int j = 0; j < this.valueCount(); j++) {
            if (this.getWholeValue(j).equals(voo))
               return j;
         }
         logger.error("wanted TreeNode (of type Value) not existent");
         return -1;
      } else {
         logger
               .error("!should not happen as TreeNode type Property can only have childen of TreeNode type Value! "
                     + "Here we have: " + node.getClass());
         return -1;
      }
   }


   @Override
   public boolean isLeaf() {
      return false;
   }
}
