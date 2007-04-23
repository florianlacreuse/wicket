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
package org.apache.wicket.extensions.breadcrumb.panel;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.breadcrumb.BreadCrumbLink;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.version.undo.Change;


/**
 * A panel that participates with a {@link IBreadCrumbModel bread crumb model}.
 * The idea is that you would have a dialog-like component that is much like a
 * wizard, but more decoupled. A typical setup is that you have a panel, where
 * the content is dynamic but hierarchical in nature, and that there are links
 * on the panel that <i>take you deeper into the hierarchy<i>
 * 
 * <p>
 * An example of using {@link BreadCrumbPanel bread crumb panels} and
 * {@link BreadCrumbLink bread crumb links}:
 * 
 * <pre>
 * add(new BreadCrumbLink(&quot;myLink&quot;, breadCrumbModel)
 * {
 * 	protected IBreadCrumbParticipant getParticipant(String componentId)
 * 	{
 * 		return new MyPanel(componentId, breadCrumbModel);
 * 	}
 * });
 * </pre>
 * 
 * where <tt>MyPanel</tt> is a {@link BreadCrumbPanel bread crumb panel} and
 * the link is added to another {@link BreadCrumbPanel bread crumb panel}
 * instance (this). When clicked, MyPanel will replace the panel that the link
 * is placed on, and it will set (and add) <tt>MyPanel</tt> as the active
 * bread crumb in the {@link IBreadCrumbModel bread crumb component model}.
 * </p>
 * 
 * @author Eelco Hillenius
 */
public abstract class BreadCrumbPanel extends Panel implements IBreadCrumbParticipant
{
	private static final long serialVersionUID = 1L;

	/** The bread crumb model. */
	private IBreadCrumbModel breadCrumbModel;

	/**
	 * Construct.
	 * 
	 * @param id
	 *            Component id
	 * @param breadCrumbModel
	 *            The bread crumb model
	 */
	public BreadCrumbPanel(String id, IBreadCrumbModel breadCrumbModel)
	{
		super(id);
		this.breadCrumbModel = breadCrumbModel;
	}

	/**
	 * Construct.
	 * 
	 * @param id
	 *            Component id
	 * @param breadCrumbModel
	 *            The bread crumb model
	 * @param model
	 *            The model
	 */
	public BreadCrumbPanel(String id, IBreadCrumbModel breadCrumbModel, IModel model)
	{
		super(id, model);
		this.breadCrumbModel = breadCrumbModel;
	}

	/**
	 * Activates the {@link BreadCrumbPanel bread crumb panel} that is the
	 * result of calling
	 * {@link IBreadCrumbPanelFactory#create(String, IBreadCrumbModel) the create method}
	 * of the bread crumb panel factory.
	 * 
	 * @param breadCrumbPanelFactory
	 */
	public void activate(IBreadCrumbPanelFactory breadCrumbPanelFactory)
	{
		activate(breadCrumbPanelFactory.create(getId(), breadCrumbModel));
	}

	/**
	 * Activates the provided participant, which typically has the effect of
	 * replacing this current panel with the one provided - as the participant
	 * typically would be a {@link BreadCrumbPanel bread crumb panel} - and
	 * updating the bread crumb model of this panel, pushing the bread crumb for
	 * the given participant on top.
	 * 
	 * @param participant
	 *            The participant to set as the active one
	 */
	public void activate(IBreadCrumbParticipant participant)
	{
		// get the currently active particpant
		final IBreadCrumbParticipant active = breadCrumbModel.getActive();
		if (active == null)
		{
			throw new IllegalStateException("The model has no active bread crumb. Before using "
					+ this + ", you have to have at least one bread crumb in the model");
		}

		// add back button support
		addStateChange(new Change()
		{
			private static final long serialVersionUID = 1L;

			public void undo()
			{
				breadCrumbModel.setActive(active);
			}
		});

		// set the bread crumb panel as the active one
		breadCrumbModel.setActive(participant);
	}

	/**
	 * Gets the bread crumb panel.
	 * 
	 * @return The bread crumb panel
	 */
	public final IBreadCrumbModel getBreadCrumbModel()
	{
		return breadCrumbModel;
	}

	/**
	 * The participating component == this.
	 * 
	 * @see org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant#getComponent()
	 */
	public Component getComponent()
	{
		return this;
	}

	/**
	 * If the previous participant is not null (and a component, which it should
	 * be), replace that component on it's parent with this one.
	 * 
	 * @see org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant#onActivate(org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant)
	 */
	public void onActivate(IBreadCrumbParticipant previous)
	{
		if (previous != null)
		{
			MarkupContainer parent = previous.getComponent().getParent();
			if (parent != null)
			{
				final String thisId = getId();
				if (parent.get(thisId) != null)
				{
					parent.replace(this);
				}
				else
				{
					// try to search downwards to match the id
					// NOTE unfortunately, we can't rely on the path pre 2.0
					Component c = (Component)parent.visitChildren(new IVisitor()
					{
						public Object component(Component component)
						{
							if (component.getId().equals(thisId))
							{
								return component;
							}
							return IVisitor.CONTINUE_TRAVERSAL;
						}
					});
					if (c == null)
					{
						// not found... do a reverse search (upwards)
						c = (Component)parent.visitParents(Component.class, new IVisitor()
						{
							public Object component(Component component)
							{
								if (component.getId().equals(thisId))
								{
									return component;
								}
								return IVisitor.CONTINUE_TRAVERSAL;
							}
						});
					}

					// replace if found
					if (c != null)
					{
						c.replaceWith(this);
					}
				}
			}
		}
		else if (getParent() != null)
		{
			getParent().replace(this);
		}
	}

	/**
	 * Sets the bread crumb panel.
	 * 
	 * @param breadCrumbModel
	 *            The bread crumb panel
	 */
	public final void setBreadCrumbModel(IBreadCrumbModel breadCrumbModel)
	{
		this.breadCrumbModel = breadCrumbModel;
	}
}