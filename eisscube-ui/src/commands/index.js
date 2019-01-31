import React, { Fragment } from 'react';
import { Field } from 'redux-form';
import {
    List,
    Create,
    Filter,
    SimpleForm,
    SimpleShowLayout,
    Datagrid,
    DateField,
    TextField,
    Labeled,
    NumberInput,
    SelectField,
    SelectInput,
    ReferenceField,
    ReferenceInput,
    AutocompleteInput,
    ShowButton,
    FormDataConsumer,
    ShowController,
    ShowView,

    required
} from 'react-admin';
import Icon from '@material-ui/icons/Message';
import { withStyles } from '@material-ui/core/styles';
import { AppDateTimeFormat, DateTimeFormat } from '../App';
import { DateTimeInput } from 'react-admin-date-inputs';
import CycleField from './CycleField';
import DutyCycleField from './DutyCycleField';
import DividerField from './DividerField';
import CycleAndDutyCycleInput from './CycleAndDutyCycleInput';

export const CommandIcon = Icon;

const styles = theme => ({
    title: {
        color: theme.palette.common.white
    },
    rowEven: {
        backgroundColor: theme.palette.grey[100]
    },
    inlineField: { 
        display: 'inline-block',
        minWidth: theme.spacing.unit * 24   
    },
    inline: { 
        display: 'inline-block',
        marginRight: theme.spacing.unit * 2, 
        minWidth: theme.spacing.unit * 32   
    },
    normalText: {
        minWidth: theme.spacing.unit * 32   
    },
    longText: {
        minWidth: theme.spacing.unit * 64   
    }
});
  
const cmds = [
    { id: 'ron', name: 'Relay ON' },
    { id: 'roff', name: 'Relay OFF' },
    { id: 'rcyc', name: 'Relay CYCLE' },
    { id: 'icp', name: 'Counting PULSE' },
    { id: 'icc', name: 'Counting CYCLE' },
    { id: 'ioff', name: 'Counting STOP' }
];

const edges = [
    { id: 'r', name: 'Raising edge' },
    { id: 'f', name: 'Falling edge' }
];

const CommandTitle = withStyles(styles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.deviceID && `${record.deviceID}`}
        </div>
    )
);

const CommandFilter = props => (
    <Filter {...props}>
        <ReferenceInput label='for EISS™Cube' source='q' reference='cubes'>
            <AutocompleteInput optionText='name'/>
        </ReferenceInput>
        <DateTimeInput label='Created Before' source='timestamp_lte' options={{ format: DateTimeFormat, ampm: false }} />
        <DateTimeInput label='Created Since' source='timestamp_gte' options={{ format: DateTimeFormat, ampm: false }} />
    </Filter>
);

export const CommandList = withStyles(styles)(
    ({ classes, ...props }) => (
        <List  
            title={<CommandTitle title='Commands' />}
            filters={<CommandFilter />}
            sort={{ field: 'created', order: 'DESC' }}
            perPage={10}
            exporter={false}
            {...props}
        >
            <Datagrid classes={{ rowEven: classes.rowEven }} >
                <DateField label='Created' source='created' showTime options={AppDateTimeFormat} />
                <ReferenceField label='EISS™Cube' source='cubeID' reference='cubes' linkType='show'>
                    <TextField source='name' />
                </ReferenceField>
                <SelectField label='Command' source='command' choices={cmds} />
                <TextField label='Status' source='status' />
                <ShowButton />
            </Datagrid>
        </List>
    )
);

export const CommandShow = withStyles(styles)(
    ({ classes, ...props }) => (
        <ShowController title={<CommandTitle title='Command' />} {...props}>
            {controllerProps => 
                <ShowView {...props} {...controllerProps}>
                    <SimpleShowLayout>
                        <SelectField className={classes.inlineField} label='Command' source='command' choices={cmds} />

                        <ReferenceField className={classes.inlineField} label='for EISS™Cube' source='cubeID' reference='cubes' linkType='show'>
                            <TextField source='name' />
                        </ReferenceField>

                        {controllerProps.record && controllerProps.record.startTime && 
                            <DateField className={classes.inlineField} label='Start date, time' source='startTime' showTime options={AppDateTimeFormat} />
                        }

                        {controllerProps.record && controllerProps.record.endTime && 
                            <DateField className={classes.inlineField} label='End date, time' source='endTime' showTime options={AppDateTimeFormat} />
                        }

                        {controllerProps.record && checkCommandForRelayCycle(controllerProps.record.command) && 
                            <Fragment>
                                <CycleField className={classes.inlineField} label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
                                <DutyCycleField className={classes.inlineField} label='Duty Cycle' source='dutyCycle' {...controllerProps}/>
                            </Fragment>
                        }

                        {controllerProps.record && checkCommandForInputCount(controllerProps.record.command) && 
                            <Fragment>
                                <Labeled label='Transition'>
                                    <SelectField className={classes.inlineField} source='transition' choices={edges} {...controllerProps}/>
                                </Labeled>
                                <CycleField className={classes.inlineField} label='Cycle' source='completeCycle' suffix='sec' {...controllerProps}/>
                            </Fragment>
                        }

                        {controllerProps.record && checkCommandForInputCycle(controllerProps.record.command) && 
                            <Fragment>
                                <Labeled label='Transition'>
                                    <SelectField className={classes.inlineField} source='transition' choices={edges} {...controllerProps}/>
                                </Labeled>
                            </Fragment>
                        }
                        
                        <DividerField />
                        
                        <TextField className={classes.inlineField} label='Status' source='status' />
                        <DateField className={classes.inlineField} label='Created on' source='created' showTime options={AppDateTimeFormat} />
                        <DateField className={classes.inlineField} label='Updated on' source='updated' showTime options={AppDateTimeFormat} />

                    </SimpleShowLayout>
                </ShowView>
            }
        </ShowController>
    )
);
      

const validateCommandCreation = (values) => {
    const errors = {};

    if (values.cycleAndDutyCycle) {
        const parts = values.cycleAndDutyCycle.split('/');
        if (parts[0] === '' || parseInt(parts[0], 10) < 1) {
            errors.cycleAndDutyCycle = ['Must be positive and more then 1 seconds'];
        }
    }

    if (values.completeCycle < 1) {
        errors.completeCycle = ['Must be positive more then 1 seconds'];
    }

    let s = (values.startTime) ? values.startTime : null;
    let e = (values.endTime) ? values.endTime : null;

    if (s !== null && e !== null) {
        let sDate = s instanceof Date ? s : new Date(s);
        let eDate = e instanceof Date ? e : new Date(e);
        if (sDate.getTime() >= eDate.getTime()) {
            errors.endTime = ['Must be after Start Time'];
        }
    }

    return errors
};

const checkCommandForRelayCycle = (v) => {
    return (v === 'rcyc') ? true : false ;
};

const checkCommandForInputCount = (v) => {
    return (v === 'icp') ? true : false ;
};

const checkCommandForInputCycle = (v) => {
    return (v === 'icc') ? true : false ;
};
            
export const CommandCreate = withStyles(styles)(
    ({ classes, ...props }) => (
        <Create 
            title={<CommandTitle title='Create new Command' />} 
            {...props}
        >
            <SimpleForm validate={ validateCommandCreation } redirect='list'>

                <ReferenceInput label='for EISS™Cube' source='cubeID' reference='cubes' validate={[ required() ]} >
                    <AutocompleteInput optionText='name'/>
                </ReferenceInput>

                <SelectInput label='Command' source='command' choices={cmds} validate={[ required() ]} />

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForRelayCycle(formData.command) &&
                    <Field name='cycleAndDutyCycle' component={CycleAndDutyCycleInput} {...rest} />
                 }
                </FormDataConsumer>

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForInputCount(formData.command) &&
                    <Fragment>
                        <SelectInput style={{ marginRight: 16 }} formClassName={classes.inline} label='Transition' source='transition' choices={edges} {...rest} validate={[ required() ]} />
                        <NumberInput formClassName={classes.inline} label='Cycle (sec)' source='completeCycle' step={'1'} {...rest} />
                    </Fragment>
                }
                </FormDataConsumer>

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForInputCycle(formData.command) &&
                    <SelectInput label='Transition' source='transition' choices={edges} {...rest} validate={[ required() ]} />
                }
                </FormDataConsumer>

                <DateTimeInput formClassName={classes.inline}
                    label='Start date, time' source='startTime' options={{ 
                        adornmentPosition: 'end',
                        format: DateTimeFormat, 
                        ampm: false, 
                        clearable: true,
                        disablePast: true
                    }} 
                />

                <DateTimeInput formClassName={classes.inline}
                    label='End date, time' source='endTime' options={{ 
                        adornmentPosition: 'end',
                        format: DateTimeFormat, 
                        ampm: false, 
                        clearable: true,
                        disablePast: true
                    }} 
                />

            </SimpleForm>
        </Create>
    )
);
