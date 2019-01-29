import React, { Fragment } from 'react';
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
import Divider from '@material-ui/core/Divider';

import { withStyles } from '@material-ui/core/styles';
import { common, grey } from '@material-ui/core/colors';

import { AppDateTimeFormat, DateTimeFormat } from '../App';
import { DateTimeInput } from 'react-admin-date-inputs';

import { Field } from 'redux-form';
import CycleField from './CycleField';
import DutyCycleField from './DutyCycleField';
import CycleAndDutyCycleInput from './CycleAndDutyCycleInput';

import sanitizeRestProps from './sanitizeRestProps';

export const CommandIcon = Icon;

const commandStyles = theme => ({
    title: {
        color: common.white
    },
    rowEven: {
        backgroundColor: grey[100]
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

const CommandTitle = withStyles(commandStyles)(
    ({classes, title, record}) => (
        <div className={classes.title}>
            {title} {record && record.deviceID && `${record.deviceID}`}
        </div>
    )
);

const CommandFilter = props => (
    <Filter {...props}>
        <ReferenceInput label='for EISS™Cube' source='q' reference='cubes'>
            <AutocompleteInput optionText='deviceID'/>
        </ReferenceInput>
        <DateTimeInput label='Created Before' source='timestamp_lte' options={{ format: DateTimeFormat, ampm: false }} />
        <DateTimeInput label='Created Since' source='timestamp_gte' options={{ format: DateTimeFormat, ampm: false }} />
    </Filter>
);

export const CommandList = withStyles(commandStyles)(
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
                    <TextField source='deviceID' />
                </ReferenceField>
                <SelectField label='Command' source='command' choices={cmds} />
                <TextField label='Status' source='status' />
                <ShowButton />
            </Datagrid>
        </List>
    )
);

export const CommandShow = withStyles(commandStyles)(
    ({ classes, ...props }) => (
        <ShowController title={<CommandTitle title='Command' />} {...props}>
            {controllerProps => 
                <ShowView {...props} {...controllerProps}>
                    <SimpleShowLayout>
                        <SelectField className={classes.inlineField} label='Command' source='command' choices={cmds} />

                        <ReferenceField className={classes.inlineField} label='for EISS™Cube' source='cubeID' reference='cubes' linkType='show'>
                            <TextField source='deviceID' />
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
                        
                        <Divider {...sanitizeRestProps(props)} />
                        
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

    if (values.completeCycle < 1) {
        errors.completeCycle = ['Must be more then 1 seconds'];
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
            
export const CommandCreate = withStyles(commandStyles)(
    ({ classes, ...props }) => (
        <Create 
            title={<CommandTitle title='Create new Command' />} 
            {...props}
        >
            <SimpleForm validate={ validateCommandCreation } redirect='list'>

                <ReferenceInput label='for EISS™Cube' source='cubeID' reference='cubes' validate={[ required() ]} >
                    <AutocompleteInput optionText='deviceID'/>
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
                        <SelectInput style={{ marginRight: 16 }} label='Transition' source='transition' choices={edges} {...rest} />
                        <NumberInput style={{ marginRight: 16 }} label='Cycle (sec)' source='completeCycle' step={'1'} {...rest} />
                    </Fragment>
                }
                </FormDataConsumer>

                <FormDataConsumer>
                {({ formData, ...rest }) => checkCommandForInputCycle(formData.command) &&
                    <SelectInput label='Transition' source='transition' choices={edges} {...rest} />
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
