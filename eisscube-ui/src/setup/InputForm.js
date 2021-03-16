import React, { Component, Fragment } from 'react';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    NumberInput,
    TextInput,
    FormDataConsumer
} from 'react-admin';
import { FormSpy } from 'react-final-form';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
});

const signals = [
    { id: 'p', name: 'Pulses' },
    { id: 'c', name: 'Cycles' }
];

const meters = [
    { id: 'e', name: 'Electric meter' },
    { id: 'g', name: 'Gas meter' }
];

const watch = [
    { id: 'r', name: 'Raising edge' },
    { id: 'f', name: 'Falling edge' }
];

const electric_units = [
    { id: 'Wh', name: 'watt-hour (Wh)' },
    { id: 'kWh', name: 'kilowatt-hour (kWh)' },
    { id: 'MWh', name: 'megawatt-hour (MWh)' }
];

const gas_units = [
    { id: 'CF', name: 'cubic feet (CF)' },
    { id: 'CCF', name: '100 x cubic feet (CCF)' },
    { id: 'MCF', name: '1000 x cubic feet (MCF)' },
    { id: 'thm', name: 'therm (thm)' },
    { id: 'Dth', name: 'dekatherm (Dth)' }
];

const InputSettings = ({ formData }) => {
    return (
        formData && formData.input && formData.input.connected ?
        <Fragment>
            <SelectInput label='Signal type' source="input.signal" choices={signals} fullWidth />

            {formData.input.signal && formData.input.signal === "p" &&
                <SelectInput label='Reading of' source="input.meter" choices={meters} fullWidth />
            }

            {formData.input.meter && formData.input.meter === "e" &&
                <SelectInput label='Unit' source="input.unit" choices={electric_units} fullWidth />
            }

            {formData.input.meter && formData.input.meter === "g" &&
                <SelectInput label='Unit' source="input.unit" choices={gas_units} fullWidth />
            }

            {formData.input.signal && formData.input.signal === "p" &&
                <NumberInput label='Factor (Pulses per Unit)' source='input.factor' fullWidth />
            }

            {formData.input.signal && formData.input.signal === "c" &&
                <SelectInput label='Watch' source="input.watch" choices={watch} fullWidth />
            }
            {formData.input.signal && formData.input.signal === "c" &&
                <NumberInput label='Load value (kW)' source='input.load' fullWidth />
            }

            <TextInput label='Label' source='input.label' fullWidth />
            <TextInput label='Description' source='input.description' fullWidth/>
        </Fragment>
        : null
    );
};

class InputForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };
    }

    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    render() {
        const { onSubmit, step, back, next } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                initialValues={data}
                save={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='input.connected' />
                <FormDataConsumer>
                    {formDataProps => <InputSettings {...formDataProps} /> }
                </FormDataConsumer>

                <FormSpy subscription={{ pristine: true, submitting: true }}>
                {props => (
                    <SetupFormButton step={step} onNext={next} onBack={back} pristine={props.pristine} submitting={props.submitting}/>
                )}
                </FormSpy>
            </SimpleForm>
        );
    }

}

InputForm.propTypes = {
    data: PropTypes.object
};

export default withStyles(styles)(InputForm);
