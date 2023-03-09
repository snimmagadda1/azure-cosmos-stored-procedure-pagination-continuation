function getItems(continuation) {
    var container = getContext().getCollection();

    container
        .chain()
        .pluck("address")
        .flatten()
        .value({
            pageSize: 100,
            continuation: continuation
        }, function (err, feed, options) {
            if (err) throw err;
            getContext().getResponse().setBody({
                result: feed,
                continuation: options.continuation
            });
        });

}