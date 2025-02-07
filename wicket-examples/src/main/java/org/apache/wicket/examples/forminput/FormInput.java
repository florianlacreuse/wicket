/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.examples.forminput;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.examples.WicketExamplePage;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.convert.MaskConverter;
import org.apache.wicket.validation.validator.RangeValidator;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Example for form input.
 *
 * @author Eelco Hillenius
 * @author Jonathan Locke
 */
public class FormInput extends WicketExamplePage
{
	/**
	 * Form for collecting input.
	 */
	private class InputForm extends Form<FormInputModel>
	{
		/**
		 * Construct.
		 *
		 * @param name
		 * 	Component name
		 */
		@SuppressWarnings("serial")
		public InputForm(String name)
		{
			super(name, new CompoundPropertyModel<>(new FormInputModel()));

			// Dropdown for selecting locale
			add(new LocaleDropDownChoice("localeSelect"));

			// Link to return to default locale
			add(new Link<Void>("defaultLocaleLink")
			{
				public void onClick()
				{
					WebRequest request = (WebRequest) getRequest();
					setLocale(request.getLocale());
				}
			});

			add(new TextField<String>("stringProperty").setRequired(true).setLabel(
				new Model<>("String")));

			add(new TextField<>("integerProperty", Integer.class).setRequired(true).add(
				new RangeValidator<>(1, Integer.MAX_VALUE)));

			add(new TextField<>("doubleProperty", Double.class).setRequired(true));

			add(new TextField<Integer>("integerInRangeProperty").setRequired(true).add(
				new RangeValidator<>(0, 100)));

			add(new CheckBox("booleanProperty"));
			add(new Multiply("multiply"));
			// display the multiply result
			Label multiplyLabel = new Label("multiplyLabel", new PropertyModel<Integer>(
				getDefaultModel(), "multiply"));
			// just for fun, add a border so that our result will be displayed as '[ x ]'
			multiplyLabel.add(new BeforeAndAfterBorder());
			add(multiplyLabel);
			RadioChoice<String> rc = new RadioChoice<>("numberRadioChoice", NUMBERS).setSuffix("");
			rc.setLabel(new Model<>("number"));
			rc.setRequired(true);
			add(rc);

			RadioGroup<String> group = new RadioGroup<>("numbersGroup");
			add(group);
			ListView<String> persons = new ListView<String>("numbers", NUMBERS)
			{
				@Override
				protected void populateItem(ListItem<String> item)
				{
					Radio<String> radio = new Radio<>("radio", item.getModel());
					radio.setLabel(item.getModel());
					item.add(radio);
					item.add(new SimpleFormComponentLabel("number", radio));
				}
			}.setReuseItems(true);
			group.add(persons);

			CheckGroup<String> checks = new CheckGroup<>("numbersCheckGroup");
			add(checks);
			ListView<String> checksList = new ListView<String>("numbers", NUMBERS)
			{
				@Override
				protected void populateItem(ListItem<String> item)
				{
					Check<String> check = new Check<>("check", item.getModel());
					check.setLabel(item.getModel());
					item.add(check);
					item.add(new SimpleFormComponentLabel("number", check));
				}
			}.setReuseItems(true);
			checks.add(checksList);

			add(new ListMultipleChoice<>("siteSelection", SITES));

			// TextField using a custom converter.
			add(new TextField<URL>("urlProperty", URL.class)
			{
				@Override
				protected IConverter<?> createConverter(Class<?> type)
				{
					if (URL.class.isAssignableFrom(type))
					{
						return URLConverter.INSTANCE;
					}
					return null;
				}
			});

			// TextField using a mask converter
			add(new TextField<UsPhoneNumber>("phoneNumberUS", UsPhoneNumber.class)
			{
				@Override
				protected IConverter<?> createConverter(Class<?> type)
				{
					if (UsPhoneNumber.class.isAssignableFrom(type))
					{
						// US telephone number mask
						return new MaskConverter<>("(###) ###-####", UsPhoneNumber.class);
					}
					return null;
				}
			});

			// and this is to show we can nest ListViews in Forms too
			add(new LinesListView("lines"));

			add(new Button("saveButton"));

			add(new Button("resetButton")
			{
				@Override
				public void onSubmit()
				{
					setResponsePage(FormInput.class);
				}
			}.setDefaultFormProcessing(false));
		}

		@Override
		public void onSubmit()
		{
			// Form validation successful. Display message showing edited model.
			info("Saved model " + getDefaultModelObject());
		}
	}

	/**
	 * list view to be nested in the form.
	 */
	private static final class LinesListView extends ListView<String>
	{
		/**
		 * Construct.
		 *
		 * @param id
		 */
		public LinesListView(String id)
		{
			super(id);
			// always do this in forms!
			setReuseItems(true);
		}

		@Override
		protected void populateItem(ListItem<String> item)
		{
			// add a text field that works on each list item model (returns
			// objects of type FormInputModel.Line) using property text.
			item.add(new TextField<>("lineEdit", new PropertyModel<String>(
				item.getDefaultModel(), "text")));
		}
	}

	/**
	 * Choice for a locale.
	 */
	private final class LocaleChoiceRenderer implements IChoiceRenderer<Locale>
	{
		@Override
		public Object getDisplayValue(Locale locale)
		{
			return locale.getDisplayName(getLocale());
		}
	}

	/**
	 * Dropdown with Locales.
	 */
	private final class LocaleDropDownChoice extends DropDownChoice<Locale>
	{
		/**
		 * Construct.
		 *
		 * @param id
		 * 	component id
		 */
		public LocaleDropDownChoice(String id)
		{
			super(id, FormInputApplication.LOCALES, new LocaleChoiceRenderer());

			// set the model that gets the current locale, and that is used for
			// updating the current locale to property 'locale' of FormInput
			setModel(new PropertyModel<>(FormInput.this, "locale"));

			add(new FormComponentUpdatingBehavior()
			{
				@Override
				protected void onUpdate()
				{
					// note that we don't have to do anything here, as our property
					// model already calls FormInput.setLocale when the model is
					// updated

					// force re-render by setting the page to render to the bookmarkable
					// instance, so that the page will be rendered from scratch,
					// re-evaluating the input patterns etc
					setResponsePage(FormInput.class);
				}
			});
		}
	}

	/** available sites for the multiple select. */
	private static final List<String> SITES = Arrays.asList("The Server Side", "Java Lobby",
		"Java.Net");

	/** available numbers for the radio selection. */
	static final List<String> NUMBERS = Arrays.asList("1", "2", "3");

	/**
	 * Constructor
	 */
	public FormInput()
	{
		// Construct form and feedback panel and hook them up
		final FeedbackPanel feedback = new FeedbackPanel("feedback")
		{
			@Override
			public void onEvent(IEvent<?> event) {
				final Object payload = event.getPayload();
				if (payload instanceof IPartialPageRequestHandler)
				{
					((IPartialPageRequestHandler) payload).add(this);
				}
			}
		};
		feedback.setOutputMarkupId(true);
		add(feedback);
		add(new InputForm("inputForm"));

		IModel<ParentFormData> model = Model.of(new ParentFormData());

		add(new ParentForm("parentForm", model));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(
			CssHeaderItem.forReference(new CssResourceReference(FormInput.class, "forminput.css")));
	}

	/**
	 * Sets locale for the user's session (getLocale() is inherited from Component)
	 *
	 * @param locale
	 * 	The new locale
	 */
	public void setLocale(Locale locale)
	{
		if (locale != null)
		{
			getSession().setLocale(locale);
		}
	}

	private static class URLConverter implements IConverter<URL>
	{
		public static final URLConverter INSTANCE = new URLConverter();

		@Override
		public URL convertToObject(String value, Locale locale)
		{
			try
			{
				return new URL(value);
			}
			catch (MalformedURLException e)
			{
				throw new ConversionException("'" + value + "' is not a valid URL");
			}
		}

		@Override
		public String convertToString(URL value, Locale locale)
		{
			return value != null ? value.toString() : null;
		}
	}

	private static class ParentForm extends Form<ParentFormData>
	{

		public ParentForm(String id, IModel<ParentFormData> model)
		{
			super(id);

			TextField<String> parentText = new TextField<>("parentText", new PropertyModel<>(model, "parentText"));
			add(parentText);

			TextArea<String> parentTextarea = new TextArea<>("parentTextarea", new PropertyModel<>(model, "parentTextarea"));
			add(parentTextarea);

			final ParentFormDataTable parentFormDataTable = new ParentFormDataTable("parentFormDataTable", model);
			add(parentFormDataTable);

			AjaxSubmitLink parentSubmit = new AjaxSubmitLink("parentSubmit")
			{

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					info("Parent form submitted");

					target.add(parentFormDataTable);
				}
			};
			add(parentSubmit);
			setDefaultButton(parentSubmit);

			Form<Void> childForm = new Form<>("childForm");
			add(childForm);

			TextField<String> childText = new TextField<>("childText", new PropertyModel<>(model, "childText"));
			childForm.add(childText);

			TextArea<String> childTextarea = new TextArea<>("childTextarea", new PropertyModel<>(model, "childTextarea"));
			childForm.add(childTextarea);

			AjaxSubmitLink childSubmit = new AjaxSubmitLink("childSubmit")
			{

				@Override
				protected void onSubmit(AjaxRequestTarget target)
				{
					info("Child form submitted");

					target.add(parentFormDataTable);
				}
			};
			childForm.add(childSubmit);
			childForm.setDefaultButton(childSubmit);
		}
	}

	private static final class ParentFormDataTable extends WebMarkupContainer
	{

		public ParentFormDataTable(String id, IModel<ParentFormData> model)
		{
			super(id);

			setOutputMarkupId(true);

			add(new Label("parentData", new PropertyModel<>(model, "parentText")));
			add(new Label("parentTextarea", new PropertyModel<>(model, "parentTextarea")));
			add(new Label("childData", new PropertyModel<>(model, "childText")));
			add(new Label("childTextarea", new PropertyModel<>(model, "childTextarea")));
		}
	}

	private static class ParentFormData implements Serializable
	{

		private String parentText;

		private String childText;

		private String parentTextarea;

		private String childTextarea;

		public String getParentText()
		{
			return parentText;
		}

		public void setParentText(String parentText)
		{
			this.parentText = parentText;
		}

		public String getChildText()
		{
			return childText;
		}

		public void setChildText(String childText)
		{
			this.childText = childText;
		}

		public String getChildTextarea()
		{
			return childTextarea;
		}

		public void setChildTextarea(String childTextarea)
		{
			this.childTextarea = childTextarea;
		}

		public String getParentTextarea()
		{
			return parentTextarea;
		}

		public void setParentTextarea(String parentTextarea)
		{
			this.parentTextarea = parentTextarea;
		}
	}
}
