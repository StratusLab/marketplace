$(document).ready(function() {

        $('.rate_image').each(function(i) {
            var image = this;
            var out_data = {
                identifier : $(image).attr('id'),
            };
            $.get(
                'ratings',
                out_data,
                function(INFO) {
                    $(image).data( 'fsr', INFO );
                    set_votes(image);
                },
                'json'
            );
        });

        $('.ratings_stars').hover(
            // Handles the mouseover
            function() {
                $(this).prevAll().andSelf().addClass('ratings_over');
                $(this).nextAll().removeClass('ratings_vote');
            },
            // Handles the mouseout
            function() {
                $(this).prevAll().andSelf().removeClass('ratings_over');
                // can't use 'this' because it wont contain the updated data
                set_votes($(this).parent());
            }
        );

        // This actually records the vote
        $('.ratings_stars').bind('click', function() {
            var star = this;
            var image = $(this).parent();

            var clicked_data = {
                clicked_on : $(star).attr('class'),
                identifier : $(star).parent().attr('id')
            };
            $.post(
                'ratings',
                clicked_data,
                function(INFO) {
                    image.data( 'fsr', INFO );
                    set_votes(image);
                },
                'json'
            );
        });

    });

    function set_votes(image) {

        var avg = $(image).data('fsr').whole_avg;
        var votes = $(image).data('fsr').number_votes;
        var exact = $(image).data('fsr').dec_avg;

        $(image).find('.star_' + avg).prevAll().andSelf().addClass('ratings_vote');
        $(image).find('.star_' + avg).nextAll().removeClass('ratings_vote');
        $(image).find('.total_votes').text( votes + ' votes recorded (' + exact + ' rating)' );
    }
