AUI.add(
	'liferay-ddm-form-field-numeric',
	function(A) {
		var Renderer = Liferay.DDM.Renderer;

		var Util = Renderer.Util;

		new A.TooltipDelegate(
			{
				position: 'left',
				trigger: '.liferay-ddm-form-field-numeric .help-icon',
				triggerHideEvent: ['blur', 'mouseleave'],
				triggerShowEvent: ['focus', 'mouseover'],
				visible: false
			}
		);

		var NumericField = A.Component.create(
			{
				ATTRS: {
					dataType: {
						value: 'integer'
					},

					placeholder: {
						state: true,
						value: ''
					},

					type: {
						value: 'numeric'
					}
				},

				EXTENDS: Liferay.DDM.Renderer.Field,

				NAME: 'liferay-ddm-form-field-numeric',

				prototype: {
					initializer: function() {
						var instance = this;

						instance.bindInputEvent('keypress', A.bind('_onNumericFieldKeyPress', instance));

						instance.evaluate = A.debounce(
							function() {
								NumericField.superclass.evaluate.apply(instance, arguments);
							},
							300
						);
					},

					getChangeEventName: function() {
						return 'input';
					},

					showErrorMessage: function() {
						var instance = this;

						NumericField.superclass.showErrorMessage.apply(instance, arguments);

						var container = instance.get('container');

						var inputGroup = container.one('.input-group-container');

						inputGroup.insert(container.one('.help-block'), 'after');
					},

					_onNumericFieldKeyPress: function(event) {
						if ((event.keyCode >= 48 && event.keyCode <= 57) || event.keyCode === 46) {
							return true;
						}

						event.preventDefault();

						return false;
					}
				}
			}
		);

		Liferay.namespace('DDM.Field').Numeric = NumericField;
	},
	'',
	{
		requires: ['aui-autosize-deprecated', 'aui-tooltip', 'liferay-ddm-form-renderer-field']
	}
);