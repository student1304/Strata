/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.convert.FromString;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.DayCount;
import com.opengamma.basics.date.HolidayCalendar;
import com.opengamma.basics.date.Tenor;
import com.opengamma.collect.ArgChecker;

/**
 * An overnight index, such as Sonia or Eonia.
 * <p>
 * An index represented by this class relates to lending over one night.
 * The rate typically refers to "Today/Tomorrow" but might refer to "Tomorrow/Next".
 * <p>
 * The index is defined by four dates.
 * The fixing date is the date on which the index is to be observed.
 * The publication date is the date on which the fixed rate is actually published.
 * The effective date is the date on which the implied deposit starts.
 * The maturity date is the date on which the implied deposit ends.
 */
@BeanDefinition(cacheHashCode = true)
public final class OvernightIndex
    implements RateIndex, ImmutableBean, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;

  /**
   * The currency of the index.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The index name, such as 'GBP-SONIA'.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String name;
  /**
   * The calendar that the index uses.
   * <p>
   * All dates are calculated with reference to the same calendar.
   */
  @PropertyDefinition(validate = "notNull")
  private final HolidayCalendar calendar;
  /**
   * The number of days to add to the fixing date to obtain the publication date.
   * <p>
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   */
  @PropertyDefinition(validate = "notNull")
  private final int publicationDateOffset;
  /**
   * The number of days to add to the fixing date to obtain the effective date.
   * <p>
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   */
  @PropertyDefinition(validate = "notNull")
  private final int effectiveDateOffset;
  /**
   * The day count convention.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code OvernightIndex} from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the index
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static OvernightIndex of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return RateIndices.ENUM_LOOKUP.lookup(uniqueName, OvernightIndex.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  @Override
  public String toString() {
    return getName();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the type of the index, which is tenor.
   * 
   * @return the index type
   */
  @Override
  public RateIndexType getType() {
    return RateIndexType.OVERNIGHT;
  }

  /**
   * Gets the tenor of the index, which is always one day.
   * 
   * @return the one day tenor
   */
  @Override
  public Tenor getTenor() {
    return Tenor.TENOR_1D;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the publication date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The publication date is the date on which the fixed rate is actually published.
   * <p>
   * An IBOR-like index is always published on the same day as the fixing date.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the publication date
   */
  @Override
  public LocalDate calculatePublicationFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    return calendar.shift(calendar.nextOrSame(fixingDate), publicationDateOffset);
  }

  /**
   * Calculates the effective date from the fixing date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid fixing date.
   * Instead, the fixing date is moved to the next valid fixing date and then processed.
   * 
   * @param fixingDate  the fixing date
   * @return the effective date
   */
  @Override
  public LocalDate calculateEffectiveFromFixing(LocalDate fixingDate) {
    ArgChecker.notNull(fixingDate, "fixingDate");
    return calendar.shift(calendar.nextOrSame(fixingDate), effectiveDateOffset);
  }

  /**
   * Calculates the fixing date from the effective date.
   * <p>
   * The fixing date is the date on which the index is to be observed.
   * The effective date is the date on which the implied deposit starts.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the fixing date
   */
  @Override
  public LocalDate calculateFixingFromEffective(LocalDate effectiveDate)  {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    return calendar.shift(calendar.nextOrSame(effectiveDate), -effectiveDateOffset);
  }

  /**
   * Calculates the maturity date from the effective date.
   * <p>
   * The effective date is the date on which the implied deposit starts.
   * The maturity date is the date on which the implied deposit ends.
   * <p>
   * No error is thrown if the input date is not a valid effective date.
   * Instead, the effective date is moved to the next valid effective date and then processed.
   * 
   * @param effectiveDate  the effective date
   * @return the maturity date
   */
  @Override
  public LocalDate calculateMaturityFromEffective(LocalDate effectiveDate) {
    ArgChecker.notNull(effectiveDate, "effectiveDate");
    return calendar.shift(calendar.nextOrSame(effectiveDate), 1);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code OvernightIndex}.
   * @return the meta-bean, not null
   */
  public static OvernightIndex.Meta meta() {
    return OvernightIndex.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(OvernightIndex.Meta.INSTANCE);
  }

  /**
   * The cached hash code, using the racy single-check idiom.
   */
  private int cachedHashCode;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static OvernightIndex.Builder builder() {
    return new OvernightIndex.Builder();
  }

  private OvernightIndex(
      Currency currency,
      String name,
      HolidayCalendar calendar,
      int publicationDateOffset,
      int effectiveDateOffset,
      DayCount dayCount) {
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notEmpty(name, "name");
    JodaBeanUtils.notNull(calendar, "calendar");
    JodaBeanUtils.notNull(publicationDateOffset, "publicationDateOffset");
    JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.currency = currency;
    this.name = name;
    this.calendar = calendar;
    this.publicationDateOffset = publicationDateOffset;
    this.effectiveDateOffset = effectiveDateOffset;
    this.dayCount = dayCount;
  }

  @Override
  public OvernightIndex.Meta metaBean() {
    return OvernightIndex.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the index.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index name, such as 'GBP-SONIA'.
   * @return the value of the property, not empty
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calendar that the index uses.
   * <p>
   * All dates are calculated with reference to the same calendar.
   * @return the value of the property, not null
   */
  public HolidayCalendar getCalendar() {
    return calendar;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the publication date.
   * <p>
   * In most cases, the fixing rate is available on the fixing date.
   * In a few cases, publication of the fixing rate is delayed until the following business day.
   * This property is zero if publication is on the fixing date, or one if it is the next day.
   * @return the value of the property, not null
   */
  public int getPublicationDateOffset() {
    return publicationDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of days to add to the fixing date to obtain the effective date.
   * <p>
   * In most cases, the settlement date and start of the implied deposit is on the fixing date.
   * In a few cases, the settlement date is the following business day.
   * This property is zero if settlement is on the fixing date, or one if it is the next day.
   * Maturity is always one business day after the settlement date.
   * @return the value of the property, not null
   */
  public int getEffectiveDateOffset() {
    return effectiveDateOffset;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * @return the value of the property, not null
   */
  @Override
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      OvernightIndex other = (OvernightIndex) obj;
      return JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getName(), other.getName()) &&
          JodaBeanUtils.equal(getCalendar(), other.getCalendar()) &&
          (getPublicationDateOffset() == other.getPublicationDateOffset()) &&
          (getEffectiveDateOffset() == other.getEffectiveDateOffset()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = cachedHashCode;
    if (hash == 0) {
      hash = getClass().hashCode();
      hash += hash * 31 + JodaBeanUtils.hashCode(getCurrency());
      hash += hash * 31 + JodaBeanUtils.hashCode(getName());
      hash += hash * 31 + JodaBeanUtils.hashCode(getCalendar());
      hash += hash * 31 + JodaBeanUtils.hashCode(getPublicationDateOffset());
      hash += hash * 31 + JodaBeanUtils.hashCode(getEffectiveDateOffset());
      hash += hash * 31 + JodaBeanUtils.hashCode(getDayCount());
      cachedHashCode = hash;
    }
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code OvernightIndex}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", OvernightIndex.class, Currency.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", OvernightIndex.class, String.class);
    /**
     * The meta-property for the {@code calendar} property.
     */
    private final MetaProperty<HolidayCalendar> calendar = DirectMetaProperty.ofImmutable(
        this, "calendar", OvernightIndex.class, HolidayCalendar.class);
    /**
     * The meta-property for the {@code publicationDateOffset} property.
     */
    private final MetaProperty<Integer> publicationDateOffset = DirectMetaProperty.ofImmutable(
        this, "publicationDateOffset", OvernightIndex.class, Integer.TYPE);
    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     */
    private final MetaProperty<Integer> effectiveDateOffset = DirectMetaProperty.ofImmutable(
        this, "effectiveDateOffset", OvernightIndex.class, Integer.TYPE);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", OvernightIndex.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "name",
        "calendar",
        "publicationDateOffset",
        "effectiveDateOffset",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case -178324674:  // calendar
          return calendar;
        case 1901198637:  // publicationDateOffset
          return publicationDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public OvernightIndex.Builder builder() {
      return new OvernightIndex.Builder();
    }

    @Override
    public Class<? extends OvernightIndex> beanType() {
      return OvernightIndex.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code calendar} property.
     * @return the meta-property, not null
     */
    public MetaProperty<HolidayCalendar> calendar() {
      return calendar;
    }

    /**
     * The meta-property for the {@code publicationDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> publicationDateOffset() {
      return publicationDateOffset;
    }

    /**
     * The meta-property for the {@code effectiveDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> effectiveDateOffset() {
      return effectiveDateOffset;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((OvernightIndex) bean).getCurrency();
        case 3373707:  // name
          return ((OvernightIndex) bean).getName();
        case -178324674:  // calendar
          return ((OvernightIndex) bean).getCalendar();
        case 1901198637:  // publicationDateOffset
          return ((OvernightIndex) bean).getPublicationDateOffset();
        case 1571923688:  // effectiveDateOffset
          return ((OvernightIndex) bean).getEffectiveDateOffset();
        case 1905311443:  // dayCount
          return ((OvernightIndex) bean).getDayCount();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code OvernightIndex}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<OvernightIndex> {

    private Currency currency;
    private String name;
    private HolidayCalendar calendar;
    private int publicationDateOffset;
    private int effectiveDateOffset;
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(OvernightIndex beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.name = beanToCopy.getName();
      this.calendar = beanToCopy.getCalendar();
      this.publicationDateOffset = beanToCopy.getPublicationDateOffset();
      this.effectiveDateOffset = beanToCopy.getEffectiveDateOffset();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 3373707:  // name
          return name;
        case -178324674:  // calendar
          return calendar;
        case 1901198637:  // publicationDateOffset
          return publicationDateOffset;
        case 1571923688:  // effectiveDateOffset
          return effectiveDateOffset;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case -178324674:  // calendar
          this.calendar = (HolidayCalendar) newValue;
          break;
        case 1901198637:  // publicationDateOffset
          this.publicationDateOffset = (Integer) newValue;
          break;
        case 1571923688:  // effectiveDateOffset
          this.effectiveDateOffset = (Integer) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public OvernightIndex build() {
      return new OvernightIndex(
          currency,
          name,
          calendar,
          publicationDateOffset,
          effectiveDateOffset,
          dayCount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code currency} property in the builder.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the {@code name} property in the builder.
     * @param name  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notEmpty(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the {@code calendar} property in the builder.
     * @param calendar  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calendar(HolidayCalendar calendar) {
      JodaBeanUtils.notNull(calendar, "calendar");
      this.calendar = calendar;
      return this;
    }

    /**
     * Sets the {@code publicationDateOffset} property in the builder.
     * @param publicationDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder publicationDateOffset(int publicationDateOffset) {
      JodaBeanUtils.notNull(publicationDateOffset, "publicationDateOffset");
      this.publicationDateOffset = publicationDateOffset;
      return this;
    }

    /**
     * Sets the {@code effectiveDateOffset} property in the builder.
     * @param effectiveDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder effectiveDateOffset(int effectiveDateOffset) {
      JodaBeanUtils.notNull(effectiveDateOffset, "effectiveDateOffset");
      this.effectiveDateOffset = effectiveDateOffset;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("OvernightIndex.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("calendar").append('=').append(JodaBeanUtils.toString(calendar)).append(',').append(' ');
      buf.append("publicationDateOffset").append('=').append(JodaBeanUtils.toString(publicationDateOffset)).append(',').append(' ');
      buf.append("effectiveDateOffset").append('=').append(JodaBeanUtils.toString(effectiveDateOffset)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
