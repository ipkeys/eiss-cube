import { useState, useEffect } from "react";
import { makeStyles, Theme } from '@material-ui/core/styles';
import { DatePicker } from "@material-ui/pickers";
import { IconButton } from "@material-ui/core";

import clsx from "clsx";
import format from "date-fns/format";
import isValid from "date-fns/isValid";
import isSameDay from "date-fns/isSameDay";
import endOfWeek from "date-fns/endOfWeek";
import startOfWeek from "date-fns/startOfWeek";
import isWithinInterval from "date-fns/isWithinInterval";
import moment from 'moment';

const useStyles = makeStyles((theme: Theme) => ({ 
    date: {
        marginTop: theme.spacing(1),
        width: theme.spacing(20)
    },
    dayWrapper: {
        position: 'relative'
    },
    day: {
        width: 36,
        height: 36,
        fontSize: theme.typography.caption.fontSize,
        margin: '0 2px',
        color: 'inherit'
    },
    customDayHighlight: {
        position: 'absolute',
        top: 0,
        bottom: 0,
        left: '2px',
        right: '2px',
        border: `1px solid ${theme.palette.secondary.main}`,
        borderRadius: '50%'
    },
    nonCurrentMonthDay: {
        color: theme.palette.text.disabled
    },
    highlightNonCurrentMonthDay: {
        color: '#676767'
    },
    highlight: {
        background: theme.palette.primary.main,
        color: theme.palette.common.white
    },
    firstHighlight: {
        extend: 'highlight',
        borderTopLeftRadius: '50%',
        borderBottomLeftRadius: '50%'
    },
    endHighlight: {
        extend: 'highlight',
        borderTopRightRadius: '50%',
        borderBottomRightRadius: '50%'
    }
}));

function makeJSDateObject(date: any) {
    if (moment.isMoment(date)) {
        return date.clone().toDate();
    }

    if (date instanceof Date) {
        return new Date(date.getTime());
    }

    return date;
}

const WeekPicker = (props: any) => {
    const { date, onChange } = props;
    const classes = useStyles();
	const [value, setValue] = useState(() => startOfWeek(makeJSDateObject(date)));

	useEffect(() => {
		setValue(startOfWeek(makeJSDateObject(date)));
	}, [date]);

	const changeDate = (new_date: any) => {
        const new_week = startOfWeek(makeJSDateObject(new_date))
		onChange(new_week);
	};

    // @ts-ignore
    const formatWeekSelectLabel = (dt, invalidLabel) => {
        let dateClone = makeJSDateObject(dt);

        return dateClone && isValid(dateClone)
            ? `Week of ${format(startOfWeek(dateClone), "MMM do")}`
            : invalidLabel;
    };

    // @ts-ignore
    const renderWrappedWeekDay = (day, selectedDate, dayInCurrentMonth) => {
        let dateClone = makeJSDateObject(day);
        let selectedDateClone = makeJSDateObject(selectedDate);

        const start = startOfWeek(selectedDateClone);
        const end = endOfWeek(selectedDateClone);

        const dayIsBetween = isWithinInterval(dateClone, { start, end });
        const isFirstDay = isSameDay(dateClone, start);
        const isLastDay = isSameDay(dateClone, end);

        const wrapperClassName = clsx({
            [classes.highlight]: dayIsBetween,
            [classes.firstHighlight]: isFirstDay,
            [classes.endHighlight]: isLastDay,
        });

        const dayClassName = clsx(classes.day, {
            [classes.nonCurrentMonthDay]: !dayInCurrentMonth,
            [classes.highlightNonCurrentMonthDay]: !dayInCurrentMonth && dayIsBetween,
        });

        return (
            <div className={wrapperClassName}>
                <IconButton className={dayClassName}>
                    <span> {format(dateClone, "d")} </span>
                </IconButton>
            </div>
        );
    };

	return (
		<DatePicker className={classes.date}
			autoOk
			variant='inline'
			label={false}
			disableFuture={true}
			value={value}
			onChange={changeDate}
            labelFunc={formatWeekSelectLabel}
            renderDay={renderWrappedWeekDay}
        />
	);
}

export default WeekPicker;
