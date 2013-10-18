package org.onetwo.common.utils.convert;

public class ToBooleanConvertor implements TypeConvert<Boolean> {

	public static final String FALSE_VALUE = "false";
	public static final String NO_VALUE = "no";

	@Override
	public Boolean convert(Object value, Class<?> componentType) {
		if (value == null)
			return false;
		Class<?> c = value.getClass();
		if (c.getSuperclass() == Number.class)
			return ((Number) value).intValue() != 0;
		if (c == Boolean.class)
			return (Boolean) value;
		if (c == Character.class)
			return ((Character) value).charValue() != 0;
		if (CharSequence.class.isAssignableFrom(c)) {
			if (FALSE_VALUE.equalsIgnoreCase(value.toString()) || NO_VALUE.equalsIgnoreCase(value.toString())) {
				return false;
			}
		}

		return true;

	}

}
