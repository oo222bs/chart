/*
	Copyright 2016 Marceau Dewilde <m@ceau.be>

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package be.ceau.chart.tests.util;

import static be.ceau.chart.tests.util.Generator.maybe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import be.ceau.chart.color.Color;
import be.ceau.chart.data.BarData;
import be.ceau.chart.data.DoughnutData;
import be.ceau.chart.data.LineData;
import be.ceau.chart.data.PieData;
import be.ceau.chart.data.RadarData;
import be.ceau.chart.dataset.BarDataset;
import be.ceau.chart.dataset.BubbleDataPoint;
import be.ceau.chart.dataset.DoughnutDataset;
import be.ceau.chart.dataset.LineDataset;
import be.ceau.chart.dataset.PieDataset;
import be.ceau.chart.dataset.RadarDataset;
import be.ceau.chart.enums.BorderSkipped;
import be.ceau.chart.enums.Easing;
import be.ceau.chart.enums.Event;
import be.ceau.chart.enums.FontFamily;
import be.ceau.chart.javascript.JavaScriptFunction;
import be.ceau.chart.options.Animation;
import be.ceau.chart.options.scales.LinearScale;
import be.ceau.chart.options.scales.LinearScales;

/**
 * Factory for randomized test instances of common Chart objects.
 */
public class TestFactory {

	public static Animation newAnimation() {
		Animation animation = new Animation();
		animation.setDuration(Generator.nextInt(5000));
		animation.setEasing(any(Easing.class));
		animation.setOnComplete(new JavaScriptFunction("function(){console.log('animation complete');}"));
		animation.setOnProgress(new JavaScriptFunction("function(){console.log('animation progress');}"));
		return animation;
	}

	public static Set<Event> getEventList() {
		Set<Event> set = EnumSet.noneOf(Event.class);
		for (int i = 0; i < Generator.nextInt(Event.values().length); i++) {
			set.add(any(Event.class));
		}
		return set;
	}
	
	public static <E> E any(Class<E> e) {
		E[] es = e.getEnumConstants();
		return es[Generator.nextInt(es.length)];
	}

	public static List<BubbleDataPoint> generateBubbleDataPoints() {
		List<BubbleDataPoint> list = new ArrayList<BubbleDataPoint>();
		
		// first choose random number of points we will generate - 4 minimum - more than 20 is overkill
		int datapoints = Generator.nextInt(20) + 5;
		
		for (int i = 4; i <= datapoints; i++) {
			
			BubbleDataPoint point = new BubbleDataPoint();
			point.setX(new BigDecimal(Generator.nextInt(500)));
			point.setY(new BigDecimal(Generator.nextInt(500)));
			point.setR(new BigDecimal(Generator.nextInt(50)));
			list.add(point);

		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public static <T> T randomInstance(Class<T> clazz) {
		// custom handlers
		if (LineData.class.equals(clazz)) {
			return (T) newLineData();
		}
		if (LinearScales.class.equals(clazz)) {
			return (T) newLinearScales();
		}
		if (PieData.class.equals(clazz)) {
			return (T) newPieData();
		}
		if (DoughnutData.class.equals(clazz)) {
			return (T) newDoughnutData();
		}
		if (RadarData.class.equals(clazz)) {
			return (T) newRadarData();
		}
		// fallback to fully generated instance
		return generatedInstance(clazz);
	}

	private static <T> T generatedInstance(Class<T> clazz) {
		T t = null;
		try {
			t = clazz.newInstance();
			for (Method method : clazz.getMethods()) {
				if (method.getName().startsWith("set")) {
					method.setAccessible(true);
					for (Class<?> clazzz : method.getParameterTypes()) {
						try {
							method.invoke(t, instance(clazzz, clazz.getSimpleName(), method.getName()));
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return t;
	}
	
	private static <T> Object instance(Class<T> t, String className, String label) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (t == null) {
			return null;
		}
		if (Boolean.class.equals(t)) {
			return maybe();
		}
		if (BigDecimal.class.equals(t)) {
			return Generator.nextBigDecimal(10);
		}
		if (Color.class.equals(t)) {
			return Color.random();
		}
		if (JavaScriptFunction.class.equals(t)) {
			return new JavaScriptFunction("function(){console.log('" + label + "');}");
		}
		if (Integer.class.equals(t)) {
			if (label.contains("Duration")) {
				return Generator.nextInt(10000);
			}
			return Generator.nextInt(10);
		}
		if (Float.class.equals(t)) {
			return Generator.nextFloat(10f);
		}
		if (String.class.equals(t)) {
			if ("setLabel".equals(label)) {
				return className + " label";
			}
			if ("setXAxisID".equals(label) || "setYAxisID".equals(label)) {
				return null;
			}
			if (label.toLowerCase(Locale.ENGLISH).contains("fontfamily")) {
				Field field = FontFamily.class.getDeclaredFields()[Generator.nextInt(FontFamily.class.getDeclaredFields().length)];
				return field.get(FontFamily.class);
			}
			return label;
		}
		if (t.isEnum()) {
			return any(t);
		}
		if (t.getPackage().getName().startsWith("be.ceau.chart")) {
			return randomInstance(t);
		}
		if (List.class.equals(t)) {
			return Collections.emptyList();
		}
		if (Set.class.equals(t)) {
			return Collections.emptySet();
		}
		if (Map.class.equals(t)) {
			return Collections.emptyMap();
		}
		if (Collection.class.equals(t)) {
			return Collections.emptyList();
		}
		return t.newInstance();
	}
	
	private static LineData newLineData() {
		LineData data = new LineData();
		LineDataset dataset = randomInstance(LineDataset.class);
		for (Entry<String, BigDecimal> entry : Generator.generateData().entrySet()) {
			data.addLabel(entry.getKey());
			dataset.addData(entry.getValue());
		}
		data.addDataset(dataset);
		return data;
	}

	private static LinearScales newLinearScales() {
		 LinearScales scales = new LinearScales();
		 scales.setxAxes(Collections.singletonList(randomInstance(LinearScale.class)));
		 scales.setyAxes(Collections.singletonList(randomInstance(LinearScale.class)));
		 return scales;
	}

	private static PieData newPieData() {
		PieData data = new PieData();
		PieDataset dataset = randomInstance(PieDataset.class);
		for (Entry<String, BigDecimal> entry : Generator.generateData().entrySet()) {
			data.addLabel(entry.getKey());
			dataset.addData(entry.getValue());
			dataset.addBackgroundColor(Color.random());
		}
		dataset.addBorderColor(Color.random());
		dataset.addBorderWidth(Generator.nextInt(14));
		dataset.addHoverBackgroundColor(Color.random());
		dataset.addHoverBorderColor(Color.random());
		dataset.addHoverBorderWidth(Generator.nextInt(14));
		data.addDataset(dataset);
		return data;
	}

	private static DoughnutData newDoughnutData() {
		DoughnutDataset dataset = new DoughnutDataset();
		DoughnutData data = new DoughnutData();
		data.addDataset(dataset);
		for (Entry<String, BigDecimal> entry : Generator.generateData().entrySet()) {
			data.addLabel(entry.getKey());
			dataset.addData(entry.getValue());
			dataset.addBackgroundColor(Color.random());
		}
		dataset.addBorderColor(Color.random());
		dataset.addBorderWidth(Generator.nextInt(22));
		dataset.addHoverBackgroundColor(Color.random());
		dataset.addHoverBorderColor(Color.random());
		dataset.addHoverBorderWidth(Generator.nextInt(22));
		return data;
	}

	private static RadarData newRadarData() {
		RadarDataset dataset = new RadarDataset();
		RadarData data = new RadarData();
		data.addDataset(dataset);
		for (Entry<String, BigDecimal> entry : Generator.generateData().entrySet()) {
			data.addLabel(entry.getKey());
			dataset.addData(entry.getValue());
		}
		return data;
	}

	public static BarData newBarData() {
		BarDataset dataset = new BarDataset();
		BarData data = new BarData();
		data.addDataset(dataset);
		for (Entry<String, BigDecimal> entry : Generator.generateData().entrySet()) {
			data.addLabel(entry.getKey());
			dataset.addData(entry.getValue());
			dataset.addBackgroundColor(Color.random());
			dataset.addBorderColor(Color.random());
			dataset.addBorderSkipped(any(BorderSkipped.class));
			dataset.addBorderWidth(Generator.nextInt(15));
			dataset.addHoverBackgroundColor(Color.random());
			dataset.addHoverBorderColor(Color.random());
			dataset.addHoverBorderWidth(Generator.nextInt(15));
		}
		dataset.setLabel("BarDataset label");
		dataset.setXAxisID("x");
		dataset.setYAxisID("y");
		return data;
	}

}
